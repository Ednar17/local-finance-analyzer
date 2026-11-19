package io.github.ednar17.finance.news;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    @Query("""
            select a from NewsArticle a
            where a.publishedAt >= :since
              and (:region is null or a.region = :region)
              and (:category is null or a.category = :category)
              and lower(a.title) like concat('%', :query, '%')
            order by a.priority asc, a.publishedAt desc
            """)
    List<NewsArticle> search(@Param("since") OffsetDateTime since,
                             @Param("region") String region,
                             @Param("category") String category,
                             @Param("query") String query,
                             Pageable pageable);

    long countByPublishedAtGreaterThanEqual(OffsetDateTime since);
}

interface NewsDigestRepository extends JpaRepository<NewsDigest, Long> {

    Optional<NewsDigest> findFirstByOrderByCreatedAtDesc();

    List<NewsDigest> findTop10ByOrderByCreatedAtDesc();
}
