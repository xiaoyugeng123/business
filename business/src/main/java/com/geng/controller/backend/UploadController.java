package com.geng.controller.backend;
import com.geng.utils.Const;
import com.geng.common.ResponseCode;
import com.geng.common.RoleEnum;
import com.geng.common.ServerResponse;
import com.geng.pojo.UserInfo;
import com.geng.vo.ImageVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
@Controller
@RequestMapping("/manage/")
public class UploadController {
    @Value("${business.imageHost}")
    private String imageHost;
    /**
     * 图片上传
     * @returnHH
     */
    @GetMapping("upload")
    public String upload(){
        return "upload";
    }
    @PostMapping("upload")
    @ResponseBody
    public ServerResponse upload(@RequestParam("uploadfile")MultipartFile uploadfile, HttpSession session){
        UserInfo user = (UserInfo) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createServerResponseByError(ResponseCode.NOT_LOGIN,"未登录");
        }
        if(user.getRole()== RoleEnum.ROLE_USER.getRole()){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"权限不足");
        }
        if(uploadfile == null || uploadfile.getOriginalFilename().equals("")){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"必须上传图片");
        }
        //获取上传的图片名称
        String oldFileName = uploadfile.getOriginalFilename();
        System.out.println("oldFileName:"+oldFileName);
        //获取文件拓展名
        String extendName = oldFileName.substring(oldFileName.lastIndexOf('.'));
        System.out.println("extendName:"+extendName);
        //生成新的文件名
        String newFilename = UUID.randomUUID().toString()+extendName;
        System.out.println("newFilename:"+newFilename);
        File mkdir = new File("F:/upload");
        if(!mkdir.exists()){
            mkdir.mkdirs();
        }
        File newFile = new File(mkdir, newFilename);
        try{
            uploadfile.transferTo(newFile);
            ImageVO imageVO = new ImageVO();
            imageVO.setUri(newFilename);
            imageVO.setUrl(imageHost+newFilename);
            return  ServerResponse.createServerResponseBySuccess(imageVO);
        }catch (IOException e){
            e.printStackTrace();
        }
        return ServerResponse.createServerResponseByError("上传失败");
    }
}