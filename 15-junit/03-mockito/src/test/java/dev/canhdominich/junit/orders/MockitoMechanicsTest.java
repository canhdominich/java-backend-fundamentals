package dev.canhdominich.junit.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.exceptions.base.MockitoAssertionError;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MockitoMechanicsTest {

    private static final List<String> REPORT = new ArrayList<>();

    @AfterAll
    static void print() {
        REPORT.forEach(System.out::println);
    }

    private static void section(String title) {
        REPORT.add("-- " + title);
    }

    private static void line(String text) {
        REPORT.add("   " + text);
    }

    private static String status(Class<?> probe) {
        List<ProbeRunner.Outcome> outcomes = ProbeRunner.run(probe);
        ProbeRunner.Outcome outcome = outcomes.get(0);
        return outcome.status() + (outcome.cause().isEmpty() ? "" : " " + outcome.cause());
    }

    @Test
    @Order(1)
    void aMockReturnsEmptyValuesUntilYouStubIt() {
        section("a mock you have not stubbed");
        StockRepository stock = mock(StockRepository.class);
        PaymentGateway payments = mock(PaymentGateway.class);
        @SuppressWarnings("unchecked")
        List<String> list = mock(List.class);
        line("int: " + stock.available("BOOK") + ", String: " + payments.charge("An", 1) + ", List.size(): " + list.size() + ", List.get(0): " + list.get(0));
        line("a method declared to return Optional gives: " + stock.shelf("BOOK") + " (not null)");
        assertThat(stock.available("BOOK")).isZero();
        assertThat(payments.charge("An", 1)).isNull();
        assertThat(list.size()).isZero();
        assertThat(stock.shelf("BOOK")).isEqualTo(Optional.empty());
    }

    @Test
    @Order(2)
    void verifyFailuresExplainWhatWasCalled() {
        section("verify with the wrong count");
        Notifier notifier = mock(Notifier.class);
        notifier.send("An", "hello");
        MockitoAssertionError error = org.junit.jupiter.api.Assertions.assertThrows(MockitoAssertionError.class, () -> verify(notifier, times(2)).send("An", "hello"));
        String message = error.getMessage().replaceAll("\\s+", " ").trim();
        line("the message says \"Wanted 2 times\" and \"But was 1 time\": " + (message.contains("Wanted 2 times") && message.contains("But was 1 time")));
        line("and it names the call that did happen: " + message.contains("notifier.send(\"An\", \"hello\")"));
        assertThat(message).contains("Wanted 2 times").contains("But was 1 time").contains("notifier.send(\"An\", \"hello\")");
    }

    @Test
    @Order(3)
    void strictStubsReportAStubThatWasNeverUsed() {
        section("a stub that the code under test never used");
        String strict = status(Probes.StrictStubs.class);
        String lenient = status(Probes.LenientStubs.class);
        line("MockitoExtension default (strict): " + strict);
        line("strictness LENIENT: " + lenient);
        assertThat(strict).isEqualTo("FAILED UnnecessaryStubbingException");
        assertThat(lenient).isEqualTo("SUCCESSFUL");
    }

    @Test
    @Order(4)
    void aSpyCallsTheRealMethodUnlessYouOverrideIt() {
        section("spy: a real object with a watcher");
        List<String> real = new ArrayList<>();
        List<String> spied = spy(real);
        spied.add("one");
        line("after add on the spy: size " + spied.size() + ", the real list size " + real.size());
        doReturn(99).when(spied).size();
        line("after doReturn(99).when(spy).size(): size " + spied.size() + ", content " + spied);
        assertThat(real).isEmpty();
        assertThat(spied.size()).isEqualTo(99);
    }

    @Test
    @Order(5)
    void finalClassesCanBeMocked() {
        section("a final class");
        TaxTable taxes = mock(TaxTable.class);
        when(taxes.percentFor("VN")).thenReturn(5);
        line("TaxTable is final; the mock answers " + taxes.percentFor("VN") + ", the real class answers " + new TaxTable().percentFor("VN"));
        assertThat(taxes.percentFor("VN")).isEqualTo(5);
    }

    @Test
    @Order(6)
    void aLooseVerifyLetsABugThrough() {
        section("the same bug (tax forgotten) against two ways of verifying");
        String lazyCorrect = status(Probes.LazyOnCorrect.class);
        String lazyBuggy = status(Probes.LazyOnBuggy.class);
        String preciseCorrect = status(Probes.PreciseOnCorrect.class);
        String preciseBuggy = status(Probes.PreciseOnBuggy.class);
        line("verify(payments).charge(anyString(), anyLong()):  correct code " + lazyCorrect + ", buggy code " + lazyBuggy);
        line("verify(payments).charge(eq(\"An\"), eq(22_000L)): correct code " + preciseCorrect + ", buggy code " + preciseBuggy);
        assertThat(lazyCorrect).isEqualTo("SUCCESSFUL");
        assertThat(lazyBuggy).isEqualTo("SUCCESSFUL");
        assertThat(preciseCorrect).isEqualTo("SUCCESSFUL");
        assertThat(preciseBuggy).startsWith("FAILED");
    }

    @Test
    @Order(7)
    void aMockOnlyAnswersWhatYouToldIt() {
        section("a mock says what you made it say");
        PaymentGateway payments = mock(PaymentGateway.class);
        when(payments.charge("An", 22_000L)).thenReturn("tx-9");
        line("the mock returns " + payments.charge("An", 22_000L) + " for exactly that call and " + payments.charge("An", 1L) + " for any other");
        assertThat(payments.charge("An", 22_000L)).isEqualTo("tx-9");
        assertThat(payments.charge("An", 1L)).isNull();
    }
}
