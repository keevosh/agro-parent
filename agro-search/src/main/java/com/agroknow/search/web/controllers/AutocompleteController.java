package com.agroknow.search.web.controllers;

import com.agroknow.search.domain.entities.AgroAutocompleteRequest;
import com.agroknow.search.domain.entities.AgroAutocompleteResponse;
import com.agroknow.search.domain.services.AutocompleteService;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author aggelos
 */
@Controller
@RequestMapping("/v1")
public class AutocompleteController {

    @Autowired
    private AutocompleteService autocompleteService;

    @RequestMapping(value = { "/_ac" }, method = { RequestMethod.GET })
    public @ResponseBody AgroAutocompleteResponse autocomplete(@RequestParam("q") String query) throws IOException {
        AgroAutocompleteRequest searchReq = new AgroAutocompleteRequest(query);
        return autocompleteService.autocomplete(searchReq);
    }
}
