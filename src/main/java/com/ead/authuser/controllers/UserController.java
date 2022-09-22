package com.ead.authuser.controllers;

import com.ead.authuser.dtos.UserDto;
import com.ead.authuser.models.UserModel;
import com.ead.authuser.services.UserService;
import com.ead.authuser.specifications.SpecificationTemplate;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Log4j2
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/users")
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserModel>> getAllUsers(
            SpecificationTemplate.UserSpec spec,
            @PageableDefault(page = 0, size = 10, sort = "userId", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) UUID courseId
    ) {

        Page<UserModel> userModelPage = null;

        if(courseId != null) {
            userModelPage = userService
                    .findAll(SpecificationTemplate.userCourseId(courseId).and(spec), pageable);
        } else {
            userModelPage = userService.findAll(spec, pageable);
        }

        if (!userModelPage.isEmpty()) {

            userModelPage
                    .forEach(userModel -> {
                        userModel
                                .add(linkTo(methodOn(UserController.class)
                                        .getOneUser(userModel.getUserId()))
                                        .withSelfRel());
                    });
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userModelPage);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Object> getOneUser(@PathVariable(value = "userId") UUID userId) {

        Optional<UserModel> userModelOptional = userService.findById(userId);

        if (!userModelOptional.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        } else {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(userModelOptional.get());
        }
    }

    @DeleteMapping("{userId}")
    public ResponseEntity<Object> deleteUser(@PathVariable(value = "userId") UUID userId) {

        log.debug("DELETE deleteUser userId received {} ", userId.toString());

        Optional<UserModel> userModelOptional = userService.findById(userId);

        if (!userModelOptional.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        } else {
            userService.delete(userModelOptional.get());

            log.debug("DELETE deleteUser userId received {} ", userId.toString());

            log.info("User deleted successfully userId {} ", userId.toString());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body("User deleted successful");
        }
    }

    @PutMapping("{userId}")
    public ResponseEntity<Object> updateUser(@PathVariable(value = "userId") UUID userId,
                                             @RequestBody
                                             @Validated(UserDto.UserView.UserPut.class)
                                             @JsonView(UserDto.UserView.UserPut.class) UserDto userDto) {

        log.debug("PUT updateUser userDto received {} ", userDto.toString());

        Optional<UserModel> userModelOptional = userService.findById(userId);

        if (!userModelOptional.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        } else {

            var userModel = userModelOptional.get();

            userModel.setFullName(userDto.getFullName());
            userModel.setPhoneNumber(userDto.getPhoneNumber());
            userModel.setCpf(userDto.getCpf());
            userModel.setLastUpdateTime(LocalDateTime.now(ZoneId.of("UTC")));

            userService.save(userModel);

            log.debug("PUT updateUser userId updated {} ", userModel.getUserId());

            log.info("User updated successfully userId {} ", userModel.getUserId());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(userModel);
        }
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<Object> updatePassword(@PathVariable(value = "userId") UUID userId,
                                                 @RequestBody
                                                 @Validated(UserDto.UserView.PasswordPut.class)
                                                 @JsonView(UserDto.UserView.PasswordPut.class) UserDto userDto) {

        log.debug("PUT updatePassword userDto received {} ", userDto.toString());

        Optional<UserModel> userModelOptional = userService.findById(userId);

        if (!userModelOptional.isPresent()) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("User not found");

        }

        if (!userModelOptional.get().getPassword().equals(userDto.getOldPassword())) {

            log.warn("Mismatched old password userId {} ", userDto.getUserId());

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Error: Mismatched old password!");
        } else {

            var userModel = userModelOptional.get();

            userModel.setPassword(userDto.getPassword());
            userModel.setLastUpdateTime(LocalDateTime.now(ZoneId.of("UTC")));

            userService.save(userModel);

            log.debug("PUT updatePassword userId saved {} ", userModel.getUserId());

            log.info("Password updated successfully userId {} ", userModel.getUserId());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body("Password updated successfully.");
        }
    }

    @PutMapping("/{userId}/image")
    public ResponseEntity<Object> updateImage(@PathVariable(value = "userId") UUID userId,
                                              @RequestBody
                                              @Validated(UserDto.UserView.ImagePut.class)
                                              @JsonView(UserDto.UserView.ImagePut.class) UserDto userDto) {

        log.debug("PUT updateImage userDto received {} ", userDto.toString());

        Optional<UserModel> userModelOptional = userService.findById(userId);

        if (!userModelOptional.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        } else {

            var userModel = userModelOptional.get();

            userModel.setImageUrl(userDto.getImageUrl());
            userModel.setLastUpdateTime(LocalDateTime.now(ZoneId.of("UTC")));

            userService.save(userModel);

            log.debug("PUT updateImage userId saved {} ", userModel.getUserId());

            log.info("Image updated successfully userId {} ", userModel.getUserId());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(userModel);
        }
    }

}
