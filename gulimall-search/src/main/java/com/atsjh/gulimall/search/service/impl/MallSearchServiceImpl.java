package com.atsjh.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atsjh.common.utils.Query;
import com.atsjh.gulimall.search.config.ElasticSearchConfig;
import com.atsjh.gulimall.search.constant.Esconstant;
import com.atsjh.gulimall.search.es.SkuEsModel;
import com.atsjh.gulimall.search.service.MallSearchService;
import com.atsjh.gulimall.search.vo.SearchParam;
import com.atsjh.gulimall.search.vo.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.omg.CORBA.IRObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: sjh
 * @date: 2021/6/30 ??????3:47
 * @description:
 */
@Slf4j
@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    RestHighLevelClient restHighLevelClient;

    /**
     * ???es??????
     * @param searchParam
     * @return
     */
    @Override
    public SearchResult search(SearchParam searchParam) {
        SearchResult result = null;
        //1????????????????????? ??????????????????????????????DSL??????
        SearchRequest searchRequest =  buildSearchRequest(searchParam);
        try {
            //2?????????????????????
            SearchResponse response = restHighLevelClient.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);
            //3??????????????????????????????????????????????????????
            result = buildSearchResult(response, searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * ??????????????????
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * @param param
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        // ??????????????????
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        /**
         * ??????????????????????????????????????????????????????????????????????????????????????????
         */
        //1. ?????? bool-query
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //1.1 bool-must ????????????
        if(!StringUtils.isEmpty(param.getKeyword())){
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        //1.2.1 bool-filter catalogId ??????????????????id??????
        if(param.getCatalog3Id() != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        //1.2.2 bool-filter brandId ????????????id??????
        if(param.getBrandId() != null && param.getBrandId().size() > 0){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        //1.2.3 bool-filter attrs ???????????????????????????
        if(param.getAttrs() != null && param.getAttrs().size() > 0){
            param.getAttrs().forEach(item ->{
                //attrs=1_5???:8???&2_16G:8G
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                String[] s = item.split("_");
                String attrId = s[0]; //??????id
                String[] attrValues = s[1].split(":");//?????????
                boolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                boolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                // ????????????????????????????????? nested ??????
                NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("attrs", boolQuery, ScoreMode.None);
                boolQueryBuilder.filter(nestedQueryBuilder);
            });
        }




        //1.2.4 bool-filter hasStock ???????????????????????????
        if(param.getHasStock() != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }
        //1.2.5 skuPrice bool-filter ????????????????????????
        if(!StringUtils.isEmpty(param.getSkuPrice())){
            //skuPrice????????????1_500???_500???500_
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if(s.length == 2){
                rangeQueryBuilder.gte(s[0]).lte(s[1]);
            }
            else if(s.length == 1){
                if(param.getSkuPrice().startsWith("_")){
                    rangeQueryBuilder.lte(s[0]);
                }
                else {
                    rangeQueryBuilder.gte(s[0]);
                }
            }
            boolQueryBuilder.filter(rangeQueryBuilder);
        }
        // ???????????????????????????
        searchSourceBuilder.query(boolQueryBuilder);


        /**
         * ????????????????????????
         */
        // 2.1 ??????  ?????????sort=hotScore_asc/desc
        if (!StringUtils.isEmpty(param.getSort())) {
            String sort = param.getSort();
            // sort=hotScore_asc/desc
            String[] sortFields = sort.split("_");

            SortOrder sortOrder = "asc".equalsIgnoreCase(sortFields[1]) ? SortOrder.ASC : SortOrder.DESC;
            searchSourceBuilder.sort(sortFields[0], sortOrder);
        }

        // 2.2 ?????? from = (pageNum - 1) * pageSize
        searchSourceBuilder.from((param.getPageNum() - 1) * Esconstant.PRODUCT_PAGE_SIZE);
        searchSourceBuilder.size(Esconstant.PRODUCT_PAGE_SIZE);

        // 2.3 ??????
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");

            searchSourceBuilder.highlighter(highlightBuilder);
        }

//        System.out.println("?????????DSL??????" + searchSourceBuilder.toString());

        /**
         * ????????????
         */
        //1. ????????????????????????
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brandAgg");
        brandAgg.field("brandId").size(50);
        //1.1 ??????????????????-???????????????
        brandAgg.subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName").size(1));
        //1.2 ??????????????????-??????????????????
        brandAgg.subAggregation(AggregationBuilders.terms("brandImgAgg").field("brandImg").size(1));

        searchSourceBuilder.aggregation(brandAgg);

        //2. ??????????????????????????????
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg");
        catalog_agg.field("catalogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        searchSourceBuilder.aggregation(catalog_agg);

        // 3. ??????????????????????????????
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        //3.1 ????????????ID????????????
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        attr_agg.subAggregation(attr_id_agg);
        //3.1.1 ???????????????ID?????????????????????????????????
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //3.1.2 ???????????????ID?????????????????????????????????
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        searchSourceBuilder.aggregation(attr_agg);

        log.info("?????????DSL?????? {}", searchSourceBuilder.toString());
        SearchRequest searchRequest = new SearchRequest(new String[]{Esconstant.PRODUCT_INDEX}, searchSourceBuilder);
        return searchRequest;
    }

    /**
     * ???es????????????
     * @param response
     * @param searchParam
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam searchParam) {
        SearchResult searchResult = new SearchResult();

        //1????????????????????????????????????
        SearchHits hits = response.getHits();
        SearchHit[] hits1 = hits.getHits();
        List<SkuEsModel> skuEsModelList = new ArrayList<>();
        if(hits1 != null && hits1.length > 0){
            for(SearchHit hit : hits1){
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                //?????????????????????
                if(!StringUtils.isEmpty(searchParam.getKeyword())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String skuTitleValue = skuTitle.getFragments()[0].toString();
                    skuEsModel.setSkuTitle(skuTitleValue);
                }
                skuEsModelList.add(skuEsModel);
            }
        }
        searchResult.setProduct(skuEsModelList);

        //2?????????????????????????????????????????????
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        List<? extends Terms.Bucket> buckets1 = attr_id_agg.getBuckets();
        ArrayList<SearchResult.AttrVo> attrVos = new ArrayList<>();
        for(Terms.Bucket bucket : buckets1){
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //??????id
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);
            //?????????
            ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attr_name_agg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);
            //?????????
            ParsedStringTerms attr_value_agg = bucket.getAggregations().get("attr_value_agg");
            List<String> attrValues = attr_value_agg.getBuckets().stream().map(item -> {
                String attr_value = ((Terms.Bucket) item).getKeyAsString();
                return attr_value;
            }).collect(Collectors.toList());
            attrVo.setAttrValue(attrValues);

            attrVos.add(attrVo);
        }
        searchResult.setAttrs(attrVos);

        //3?????????????????????????????????????????????
        ParsedLongTerms aggregations = response.getAggregations().get("brandAgg");
        List<? extends Terms.Bucket> buckets = aggregations.getBuckets();
        ArrayList<SearchResult.BrandVo> brandVos = new ArrayList<>();
        for(Terms.Bucket bucket : buckets){
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //?????????id
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);
            //?????????
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);
            //??????????????????
            ParsedStringTerms brandImgAgg = bucket.getAggregations().get("brandImgAgg");
            String brandImg = brandImgAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }
        searchResult.setBrands(brandVos);


        //4?????????????????????????????????????????????
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<? extends Terms.Bucket> buckets2 = catalog_agg.getBuckets();
        ArrayList<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        for(Terms.Bucket bucket : buckets2){
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //??????id
            long catelogId = bucket.getKeyAsNumber().longValue();
            catalogVo.setCatalogId(catelogId);
            //?????????
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catelogName = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catelogName);

            catalogVos.add(catalogVo);
        }
        searchResult.setCatalogs(catalogVos);

        //5???????????????-??????, ????????????
        searchResult.setPageNum(searchParam.getPageNum());
        searchResult.setTotal(response.getHits().getTotalHits().value);
        searchResult.setTotalPages((int) Math.ceil(searchResult.getTotal()*1.0/Esconstant.PRODUCT_PAGE_SIZE));


        //?????????????????????
        if(searchParam.getAttrs() != null && searchParam.getAttrs().size() > 0){
            List<SearchResult.NavVo> collect = searchParam.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                //attrs=1_5???:8??? & 2_16G:8G
                String[] s = attr.split("_");
                long attrId = Long.parseLong(s[0]);
                //??????attrId????????????
                List<SearchResult.AttrVo> attrs = searchResult.getAttrs();
                for (SearchResult.AttrVo vo : attrs) {
                    if (vo.getAttrId().equals(attrId)) {
                        navVo.setNavName(vo.getAttrName());
                        break;
                    }
                }
                navVo.setNavValue(s[1]);
                //??????????????????????????????????????????
                String encode = URLEncoder.encode(attr);
                encode.replace("+", "%20");
                String link = searchParam.get_queryString().replace("&attrs=" + encode, "");
                navVo.setLink("http://search.gulimall.com/list.html?"+link);
                return navVo;
            }).collect(Collectors.toList());
            searchResult.setNavs(collect);
        }

        //????????????????????????
        if(searchParam.getBrandId() != null && searchParam.getBrandId().size() > 0){
            if(searchResult.getNavs() == null){
                searchResult.setNavs(new ArrayList<SearchResult.NavVo>());
            }
            List<Long> brandId = searchParam.getBrandId();
            List<SearchResult.NavVo> brandNavs = brandId.stream().map(id -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                navVo.setNavName("??????");
                //???????????????
                for (SearchResult.BrandVo vo : searchResult.getBrands()) {
                    if (vo.getBrandId() == id) {
                        navVo.setNavValue(vo.getBrandName());
                        break;
                    }
                }
                String replace = searchParam.get_queryString().replace("&brandId=" + id, "");
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);
                return navVo;
            }).collect(Collectors.toList());
            searchResult.getNavs().addAll(brandNavs);
        }

        return searchResult;
    }


}
