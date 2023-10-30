package com.yupi.controller;
import com.yupi.pojo.ClientUser;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/name")
public class NameController {

//    @GetMapping("")
//    public String getNameByGet(String name){
//        return "get " + name;
//    }
//
//    @PostMapping("")
//    public String getNameByPost(String name){
//        return "post " + name;
//    }

    @PostMapping("/user")
    public String getNameByJson(@RequestBody ClientUser clientUser, HttpServletRequest request){
        String Tag = null;
        Tag = request.getHeader("Tag");
        if (Tag == null){
            throw new RuntimeException("无权限");
        }
        if (!Tag.equals("api")){
            throw new RuntimeException("无权限");
        }
        return "json " + clientUser.getName();
    }
}
