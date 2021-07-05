package com.atsjh.gulimall.search.controller;

import com.atsjh.gulimall.search.service.MallSearchService;
import com.atsjh.gulimall.search.vo.SearchParam;
import com.atsjh.gulimall.search.vo.SearchResult;
import com.netflix.client.http.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * @author: sjh
 * @date: 2021/6/30 下午3:01
 * @description:
 */
@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    /**
     * 搜索， 将搜索条件封装成对象
     * @param searchParam
     * @param model
     * @return
     */
    @GetMapping("/list.html")
    public String toList(SearchParam searchParam, Model model, HttpServletRequest request){
        //获取完整的请求
        String queryString = request.getQueryString();
        searchParam.set_queryString(queryString);

        SearchResult searchResult = mallSearchService.search(searchParam);
        model.addAttribute("result", searchResult);
        return "list";
    }
}
