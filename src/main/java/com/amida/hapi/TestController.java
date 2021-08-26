package com.amida.hapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    //@Autowired
    //private HttpServletRequest request;

    @GetMapping(value = "/hello")
    public String helloWorld() {
        return "Hello World";
    }

    /*@GetMapping(value = "/patient/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getPatient(@PathVariable("id") long id, Model model) {
        return String.valueOf(id);
    }

    @DeleteMapping(value = "/patient/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String deletePatient(@PathVariable("id") long id, Model model) {
        return String.valueOf(id);
    }

    @GetMapping(path = "/username")
    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public ResponseEntity<String> getAuthorizedUserName() {
        return ResponseEntity.ok(SecurityContextUtils.getUserName());
    }

    @GetMapping(path = "/roles")
    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public ResponseEntity<Set<String>> getAuthorizedUserRoles() {
        return ResponseEntity.ok(SecurityContextUtils.getUserRoles());
    }*/
}
