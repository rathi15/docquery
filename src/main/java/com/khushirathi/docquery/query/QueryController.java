package com.khushirathi.docquery.query;

import com.khushirathi.docquery.auth.CurrentUser;
import com.khushirathi.docquery.query.dto.QueryRequest;
import com.khushirathi.docquery.query.dto.QueryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @PostMapping
    public QueryResponse query(@Valid @RequestBody QueryRequest request) {
        return queryService.answer(request, CurrentUser.id());
    }
}