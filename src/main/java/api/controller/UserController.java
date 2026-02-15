package api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.dto.CreateUserRequest;
import api.dto.GetUsersRequest;
import core.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<String> createUser(@Valid @RequestBody CreateUserRequest request) {
        boolean isCreated = userService.addUser(request.toNewUser());
        if (!isCreated) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("User with this email already exists.");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("User created successfully.");
    }

    @GetMapping
    public ResponseEntity<List<GetUsersRequest>> getAllUsers() {
        List<GetUsersRequest> response = userService.getAllUsers();
        return ResponseEntity.ok(response);
    }
}
