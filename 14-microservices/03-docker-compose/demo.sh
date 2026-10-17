set -euo pipefail
cd "$(dirname "$0")"

INVENTORY=http://localhost:18081
ORDER=http://localhost:18080
SIMPLE=http://localhost:18082
SRC=inventory-service/src/main/java/dev/canhdominich/micro/inventory/InventoryController.java

size() { docker images --format '{{.Size}}' "$1"; }
seconds() { python3 -c "print(round($2 - $1))"; }
layer_size() { docker history "$1" --format '{{.Size}} {{.CreatedBy}}' | grep "$2" | head -1 | cut -d' ' -f1; }
cached_copy() {
  awk -v pattern="$2" '
    index($0, pattern) { id = $1 }
    $1 == id && /CACHED/ { found = 1 }
    END { print (found ? "yes" : "no") }' "$1"
}

mvn -q -DskipTests package
docker compose --profile compare build -q inventory order inventory-simple >/dev/null

echo "-- image"
echo "layered image: $(size micro-inventory:lab)"
echo "layer with the libraries: $(layer_size micro-inventory:lab 'extracted/dependencies')"
echo "layer with your classes: $(layer_size micro-inventory:lab 'extracted/application')"
echo "single-layer image copies the whole jar in one layer: $(layer_size micro-inventory-simple:lab 'target/inventory-service.jar')"

echo "-- after changing one line of code"
cp "$SRC" /tmp/InventoryController.java.bak
trap 'cp /tmp/InventoryController.java.bak "$SRC"' EXIT
sed -i 's/return "reserved";/return "reserved and logged";/' "$SRC"
mvn -q -DskipTests package
docker compose build inventory --progress=plain > /tmp/layered-build.log 2>&1
docker compose --profile compare build inventory-simple --progress=plain > /tmp/simple-build.log 2>&1
echo "layered build reused the libraries layer: $(cached_copy /tmp/layered-build.log 'extracted/dependencies')"
echo "single-layer build reused the jar layer: $(cached_copy /tmp/simple-build.log 'target/inventory-service.jar')"
cp /tmp/InventoryController.java.bak "$SRC"
mvn -q -DskipTests package
docker compose --profile compare build -q inventory order inventory-simple >/dev/null

echo "-- start the system"
start=$(date +%s.%N)
docker compose --profile compare up -d --wait >/dev/null 2>&1
echo "healthy after about $(seconds "$start" "$(date +%s.%N)") s"
echo "order service reaches inventory at: $(curl -s $ORDER/where-is-inventory)"
echo "order through the compose network: status $(curl -s -o /dev/null -w '%{http_code}' -X POST "$ORDER/orders?sku=BOOK&quantity=2"), stock now $(curl -s $INVENTORY/stock/BOOK)"
echo "inventory is also reachable from the host: $(curl -s -o /dev/null -w '%{http_code}' $INVENTORY/stock/BOOK)"

echo "-- memory limit 256 MB"
echo "with -XX:MaxRAMPercentage=75: $(curl -s $INVENTORY/memory)"
echo "with default JVM options: $(curl -s $SIMPLE/memory)"
docker compose --profile compare stop inventory-simple >/dev/null 2>&1

echo "-- stopping a service that is busy with a 5 s request"
for mode in graceful immediate; do
  INVENTORY_SHUTDOWN=$mode docker compose up -d --wait inventory >/dev/null 2>&1
  curl -s -o /dev/null -w '%{http_code}' "$INVENTORY/slow?millis=5000" > /tmp/slow-status.txt 2>/dev/null || true &
  sleep 1
  begin=$(date +%s.%N)
  docker compose stop -t 20 inventory >/dev/null 2>&1
  waited=$(seconds "$begin" "$(date +%s.%N)")
  wait
  echo "shutdown $mode: stop took about $waited s, the request got status $(cat /tmp/slow-status.txt), container exit code $(docker inspect -f '{{.State.ExitCode}}' "$(docker compose ps -a -q inventory)")"
done

docker compose --profile compare down >/dev/null 2>&1
