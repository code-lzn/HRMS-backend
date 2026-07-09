package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.limou.hrms.model.entity.Post;
import com.limou.hrms.service.PostService;
import com.limou.hrms.mapper.PostMapper;
import org.springframework.stereotype.Service;

/**
* @author henan
* @description 针对表【post(帖子)】的数据库操作Service实现
* @createDate 2026-06-27 21:02:44
*/
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
    implements PostService{

}




