package eu.h2020.symbiote.administration.model.mappers;

import eu.h2020.symbiote.security.commons.enums.UserRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserRoleValueTextMapping {

    private String enumValue;
    private String enumText;

    public UserRoleValueTextMapping() {
    }

    public UserRoleValueTextMapping(String enumValue, String enumText) {
        setEnumValue(enumValue);
        setEnumText(enumText);
    }

    public String getEnumValue() { return enumValue; }
    public void setEnumValue(String enumValue) { this.enumValue = enumValue; }

    public String getEnumText() { return enumText; }
    public void setEnumText(String enumText) { this.enumText = enumText; }

    public boolean isSelected(String enumValue){
        if (enumValue != null) {
            return this.enumValue.equals(enumValue);
        }
        return false;
    }

    public static List<UserRoleValueTextMapping> getList() {
        List<UserRoleValueTextMapping> list = new ArrayList<>();

        List<String> allRoles = Stream.of(UserRole.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        allRoles.remove("NULL");

        for (String role : allRoles) {
            List<String> parts = new ArrayList<>(Arrays.asList(role.split("_")));
            parts.replaceAll(p -> p.substring(0, 1) + p.substring(1).toLowerCase());

            if (parts.size() > 1)
                list.add(new UserRoleValueTextMapping(role, String.join(" ", parts)));
            else
                list.add(new UserRoleValueTextMapping(role, parts.get(0)));
        }

        list.add(0, new UserRoleValueTextMapping("NULL", "Choose your User Role"));
        return list;
    }
}
