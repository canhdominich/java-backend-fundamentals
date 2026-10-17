package dev.canhdominich.boot.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class QueryBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() {
        lines = QueryDemo.run(MySqlLab.url(), MySqlLab.user(), MySqlLab.password());
    }

    private static List<String> block(String prefix) {
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) {
                start = i;
                break;
            }
        }
        assertThat(start).as(prefix).isGreaterThanOrEqualTo(0);
        int end = start + 1;
        while (end < lines.size() && lines.get(end).startsWith("    ")) {
            end++;
        }
        return lines.subList(start, end);
    }

    @Test
    void derivedQueriesTurnKeywordsIntoSql() {
        assertThat(block("findByTitleContainingIgnoreCase").get(1)).contains("where upper(c1_0.title) like upper(?)");
        assertThat(block("findByPriceVndBetween").get(1)).endsWith("where c1_0.price_vnd between ? and ?");
        assertThat(block("findByLevelInOrderByPriceVndDesc").get(1)).endsWith("where c1_0.level in (?,?) order by c1_0.price_vnd desc");
        assertThat(block("findTop3ByOrderByPriceVndAsc").get(1)).endsWith("order by c1_0.price_vnd limit ?");
        assertThat(block("countByLevel").get(1)).isEqualTo("    select count(c1_0.id) from course c1_0 where c1_0.level=?");
        assertThat(block("existsByTitle").get(1)).endsWith("where c1_0.title=? limit ?");
    }

    @Test
    void jpqlAndNativeQueriesRunAsWritten() {
        assertThat(block("JPQL expensiveIn").get(1)).endsWith("where c1_0.category=? and c1_0.price_vnd>=?");
        assertThat(block("native cheapestTwoNative").get(1)).isEqualTo("    select * from course where category = ? order by price_vnd limit 2");
    }

    @Test
    void thePageReturnTypeCostsASecondCountQueryAndTheOthersDoNot() {
        assertThat(block("Page: findByLevel").get(0)).contains("content 5, total 20, pages 4, number 1").endsWith("2 statements");
        assertThat(block("Slice:").get(0)).contains("hasNext true").endsWith("1 statements");
        assertThat(block("List: findListByLevel").get(0)).endsWith("1 statements");
        assertThat(block("Page with size 100").get(0)).contains("content 20, total 20").endsWith("1 statements");
    }

    @Test
    void sortingBecomesOrderByAndUnknownPropertiesFailFast() {
        assertThat(block("page 0, size 3, sort by priceVnd descending").get(1)).contains("order by c1_0.price_vnd desc limit ?");
        assertThat(lines).contains("sort by an unknown property -> PropertyReferenceException: PropertyReferenceException: No property 'nope' found for type 'Course'");
    }

    @Test
    void projectionsSelectOnlyTheColumnsTheyNeed() {
        assertThat(block("entity: findListByLevel").get(1)).startsWith("    select c1_0.id,c1_0.category,c1_0.level,c1_0.price_vnd,c1_0.title from");
        assertThat(block("interface projection").get(1)).isEqualTo("    select c1_0.title from course c1_0 where c1_0.level=?");
        assertThat(block("record projection").get(1)).isEqualTo("    select c1_0.title,c1_0.price_vnd from course c1_0 where c1_0.level=?");
        assertThat(block("dynamic projection").get(1)).isEqualTo("    select c1_0.title from course c1_0 where c1_0.category=?");
    }

    @Test
    void aBulkUpdateLeavesTheLoadedEntityStaleUnlessTheContextIsCleared() {
        assertThat(lines).contains("stale entity after a bulk update inside one transaction: price before 130000, rows updated 10, price read again 130000");
        assertThat(lines.stream().filter(l -> l.startsWith("stale entity with clearAutomatically")).findFirst().orElseThrow()).endsWith("price before 130001, rows updated 10, price read again 130002");
    }

    @Test
    void aMisspelledRepositoryMethodStopsStartup() {
        assertThat(lines.get(lines.size() - 1)).startsWith("startup failed: BeanCreationException / PropertyReferenceException: No property 'titel' found for type 'Course'");
    }
}
