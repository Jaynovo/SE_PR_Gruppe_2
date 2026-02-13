package at.jku.se.gruppe2.presentation.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the pure-Java methods of {@link UIUtils}.
 *
 * <p>JavaFX-dependent methods ({@code styledAlert}, {@code styledTextInputDialog},
 * {@code styledChoiceDialog}, {@code styledConfirm}, {@code showAlarmPopup},
 * {@code setupCountryComboBox}) require a running JavaFX toolkit and are not
 * covered here.</p>
 *
 * <p>These tests verify the deterministic, toolkit-free behaviour of
 * {@link UIUtils#getCountryList()}.</p>
 */
class UIUtilsTest {

    // -------------------------------------------------------------------------
    // getCountryList()
    // -------------------------------------------------------------------------

    @Test
    void getCountryList_isNotNull() {
        assertNotNull(UIUtils.getCountryList());
    }

    @Test
    void getCountryList_isNotEmpty() {
        assertFalse(UIUtils.getCountryList().isEmpty());
    }

    @Test
    void getCountryList_isSortedAlphabetically() {
        List<String> countries = UIUtils.getCountryList();
        for (int i = 0; i < countries.size() - 1; i++) {
            assertTrue(
                    countries.get(i).compareToIgnoreCase(countries.get(i + 1)) <= 0,
                    "Expected '" + countries.get(i) + "' to come before '" + countries.get(i + 1) + "'"
            );
        }
    }

    @Test
    void getCountryList_containsWellKnownCountries() {
        List<String> countries = UIUtils.getCountryList();
        assertTrue(countries.contains("Österreich"),  "Should contain Österreich");
        assertTrue(countries.contains("Frankreich"),   "Should contain Frankreich");
        assertTrue(countries.contains("Japan"),    "Should contain Japan");
    }

    @Test
    void getCountryList_containsNoNullOrBlankEntries() {
        for (String country : UIUtils.getCountryList()) {
            assertNotNull(country, "Country list must not contain null entries");
            assertFalse(country.isBlank(), "Country list must not contain blank entries");
        }
    }

    @Test
    void getCountryList_returnsSameInstanceOnRepeatedCalls() {
        List<String> first  = UIUtils.getCountryList();
        List<String> second = UIUtils.getCountryList();
        assertSame(first, second, "getCountryList() should return the cached instance");
    }

    @Test
    void getCountryList_hasSizeLargerThan30() {
        // ISO 3166-1 defines 249 country codes; at a minimum we expect a reasonable subset
        assertTrue(UIUtils.getCountryList().size() > 30,
                "Country list should contain more than 30 entries");
    }

    @Test
    void getCountryList_containsNoDuplicates() {
        List<String> countries = UIUtils.getCountryList();
        long distinct = countries.stream().distinct().count();
        assertEquals(countries.size(), distinct, "Country list must not contain duplicates");
    }
}