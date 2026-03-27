package api.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.dto.GetUsersRequest;
import api.dto.UserAccountSummaryResponse;
import core.service.AccountSummaryService;
import core.service.UserService;

@RestController
@RequestMapping("/v1/users")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private AccountSummaryService accountSummaryService;

    @GetMapping
    public ResponseEntity<List<GetUsersRequest>> getAllUsers() {
        List<GetUsersRequest> response = userService.getAllUsers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/summary")
    public ResponseEntity<UserAccountSummaryResponse> getAccountSummary(Principal principal) {
        return ResponseEntity.ok(accountSummaryService.getSummary(principal.getName()));
    }
}
