package com.atsjh.gulimall.search.service;

import com.atsjh.gulimall.search.vo.SearchParam;
import com.atsjh.gulimall.search.vo.SearchResult;

/**
 * @author: sjh
 * @date: 2021/6/30 下午3:46
 * @description:
 */
public interface MallSearchService {
    SearchResult search(SearchParam searchParam);
}
