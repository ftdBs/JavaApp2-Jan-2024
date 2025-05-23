package org.csystem.app.gis.wiki.data.repository;

import org.csystem.app.gis.wiki.data.dal.WikiSearchDataHelper;
import org.csystem.app.gis.wiki.data.mapper.IWikiSearchInfoMapper;
import org.csystem.app.gis.wiki.data.mapper.WikiSearchInfoInfoMapper;
import org.csystem.app.gis.wiki.geonames.service.GeonamesWikiSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootApplication
@SpringBootTest
@EntityScan(basePackages = "org.csystem.app.gis.wiki.data.entity")
@ComponentScan(basePackages = "org.csystem")
@EnableJpaRepositories
@TestPropertySource(locations = "classpath:application-unittest.properties")
public class WikiSearchInfoTest {
    @Autowired
    private WikiSearchDataHelper m_wikiSearchDataHelper;

    @Autowired
    private IWikiSearchInfoRepository m_wikiSearchInfoRepository;

    @Autowired
    private GeonamesWikiSearchService m_geonamesWikiSearchService;

    private final IWikiSearchInfoMapper m_wikiSearchInfoMapper = new WikiSearchInfoInfoMapper(new ModelMapper());

    @BeforeEach
    public void setUp()
    {
        var queryText = "zonguldak";
        var geonamesWikiSearch = m_geonamesWikiSearchService.findWikiSearchInfo(queryText, 1000);
        var wikiSearchInfo = geonamesWikiSearch.stream().map(m_wikiSearchInfoMapper::toWikiSearchInfo).toList();

        m_wikiSearchDataHelper.saveWikiSearchInfo(queryText, wikiSearchInfo);
    }

    @Test
    public void test()
    {
        var queryText = "zonguldak";
        var datePerPage = 5;
        var pageNumber = 1; //second page
        var dataIndex = 1;
        var expectedSummaryText = "Monfalcone";

        var pageable = PageRequest.of(pageNumber, datePerPage);
        var result = m_wikiSearchInfoRepository.findByQueryText(queryText, pageable);

        assertFalse(result.isEmpty());
        assertTrue(result.getContent().get(dataIndex).summary.startsWith(expectedSummaryText));
    }
}
