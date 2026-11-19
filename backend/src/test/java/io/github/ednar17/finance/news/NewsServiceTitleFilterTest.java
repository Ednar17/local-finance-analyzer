package io.github.ednar17.finance.news;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NewsServiceTitleFilterTest {

    @Test
    void nullOrBlankQueryMatchesEveryTitle() {
        assertThat(NewsService.titleFilter(null)).isEmpty();
        assertThat(NewsService.titleFilter("   ")).isEmpty();
    }

    @Test
    void queryIsTrimmedAndLowerCased() {
        assertThat(NewsService.titleFilter("  Sanctions UE ")).isEqualTo("sanctions ue");
    }
}
