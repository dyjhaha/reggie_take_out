package com.itheima.test;

import org.junit.jupiter.api.Test;

public class UploadFileTest {

    @Test
    public void test1(){
        String fileName="haaa.jpg";
        String substring = fileName.substring(fileName.lastIndexOf("."));
        System.out.println(substring);

    }
}
