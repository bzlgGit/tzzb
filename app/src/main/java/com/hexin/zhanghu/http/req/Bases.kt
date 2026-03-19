package com.hexin.zhanghu.http.req

import com.google.gson.annotations.SerializedName
import com.hexin.zhanghu.http.retrofit.annotation.API

open  class BaseReq(
    @SerializedName("terminal")
    val terminal:String = "0",
    @SerializedName("version")
    val version: String = "0"
)

open  class BaseT(
    @SerializedName("error_code")
    val errorCode:String = "0",
    @SerializedName("error_msg")
    val errorMsg: String = "0"
)