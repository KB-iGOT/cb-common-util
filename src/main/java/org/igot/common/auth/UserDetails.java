package org.igot.common.auth;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class UserDetails {
    private String designation;
    private String designations;
    private String group;
    private String name;
    private String org;
    private String userId;
    private List<String> userRoles;
}
