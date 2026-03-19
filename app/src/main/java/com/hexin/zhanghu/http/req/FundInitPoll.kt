package com.hexin.zhanghu.http.req

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.hexin.zhanghu.http.RetrofitManager
import com.hexin.zhanghu.http.retrofit.annotation.API
import com.hexin.zhanghu.http.retrofit.annotation.Encrypt
import com.hexin.zhanghu.http.retrofit.encrypt.EncryptComponent.Companion.AES
import com.hexin.zhanghu.http.retrofit.encrypt.EncryptComponent.Companion.PROTOCOL_KEY
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.POST


/**
 * 接口文档
 * {"errcode":0,"errmsg":"成功！","data":{"query_path":{"path":"/common/fund_cal_polling","params":[]},"protocol":"http","isArsenal":false,"serviceId":0,"serviceName":"","select_business":"-100024145","select_businessname":"thsxcs-dubbo-caishen-git","applicationid":"-100024145","applicationname":"thsxcs-dubbo-caishen-git","nocover":1,"arsenalUrl":"","edit_uid":757,"status":"abandon","type":"static","req_body_is_json_schema":true,"res_body_is_json_schema":true,"api_opened":false,"index":1,"tag":["接口测试-支持自动化"],"operate_type":"query","cooperate_mode":"request-response","_id":38556,"method":"GET","catid":363058,"title":"基金账户计算轮询接口","path":"/common/fund_cal_polling","project_id":1526,"req_params":[],"res_body_type":"json","req_query":[{"is_constant":"0","required":"1","_id":"651132a564dbb20108230a1a","name":"userid","example":"9876","desc":"userid"},{"is_constant":"0","required":"1","_id":"651132a564dbb20108230a19","name":"page_id","example":"fund_account","desc":"页面id  不同页面不同id"},{"is_constant":"0","required":"0","_id":"651132a564dbb20108230a18","name":"fundid","example":"132456","desc":"fundid 账户id ,如果用户维度则不传"},{"is_constant":"0","required":"0","_id":"651132a564dbb20108230a17","name":"account_type","example":"fund","desc":"写死fund 可选传"},{"is_constant":"0","required":"0","_id":"651132a564dbb20108230a16","name":"version","example":"1","desc":"版本"},{"is_constant":"0","required":"0","_id":"651132a564dbb20108230a15","name":"terminal","example":"1","desc":"设备"},{"is_constant":"0","required":"0","_id":"651132a564dbb20108230a14","name":"id","example":"1","desc":"h5id 原样返回"},{"is_constant":"0","required":"0","_id":"651132a564dbb20108230a13","name":"polling_times","query_type":"string","example":"10","desc":"客户端本次轮询次数"}],"req_headers":[],"req_body_form":[],"markdown":"<span class=\"colour\" style=\"color:rgb(85, 85, 85)\">判断当前页面数据是否准备完成</span>\n1、通过fundid生成对应账号数据状态的key，从redis中获取状态\n2、通过页面id判断当前页面数据是否已经和计算模块当前计算状态匹配。\n\n计算完成后可查询最新的持仓数据\n\npage_id约定 :\nfund_assets  //基金持仓页\nfund_index   //基金首页\n\n{\n  \"error_code\": \"0\",\n  \"error_msg\": \"success\",\n  \"ex_data\": [\n                        {\n                           \"fundid\":1234,\n                           \"status\":0,   // 0 计算成功  1计算中 -1计算失败\n                        },\n                        {\n                           \"fundid\":12345,\n                           \"status\":0,   //\n                        }\n                   ]\n}","desc":"<p><span class=\"colour\" style=\"color:rgb(85, 85, 85)\">判断当前页面数据是否准备完成</span><br>\n1、通过fundid生成对应账号数据状态的key，从redis中获取状态<br>\n2、通过页面id判断当前页面数据是否已经和计算模块当前计算状态匹配。</p>\n<p>计算完成后可查询最新的持仓数据</p>\n<p>page_id约定 :<br>\nfund_assets&nbsp; //基金持仓页<br>\nfund_index&nbsp; &nbsp;//基金首页</p>\n<p>{<br>\n&nbsp; \"error_code\": \"0\",<br>\n&nbsp; \"error_msg\": \"success\",<br>\n&nbsp; \"ex_data\": [<br>\n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; {<br>\n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; \"fundid\":1234,<br>\n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; \"status\":0,&nbsp;&nbsp; // 0 计算成功&nbsp; 1计算中 -1计算失败<br>\n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; },<br>\n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; {<br>\n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; \"fundid\":12345,<br>\n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; \"status\":0,&nbsp;&nbsp; //<br>\n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; }<br>\n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ]<br>\n}</p>\n","res_body":"{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"object\",\"properties\":{\"error_code\":{\"type\":\"string\",\"description\":\"错误码\"},\"error_msg\":{\"type\":\"string\",\"description\":\"错误信息\"},\"ex_data\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"fundid\":{\"type\":\"number\",\"description\":\"fid\"},\"status\":{\"type\":\"number\",\"description\":\"状态\"}},\"description\":\"数据\"},\"description\":\"响应内容\"}}}","uid":757,"add_time":1603689419,"up_time":1695625896,"__v":0,"service_program_id":321586,"code_list":[{"_id":"64d59e17fcd0d101089d62a7","code":"0","value":"成功","desc":"成功"}],"domain_path":[{"_id":"689a9667805a0a0100da8836","env":"test","domain":"testcapital.hexin.cn","path":"/caishen_fund"},{"_id":"689a9667805a0a0100da8835","env":"pre","domain":"pretestcapital.hexin.cn","path":"/caishen_fund"}],"username":"wudi","edit_username":"wudi","service_program_data":{"service_identifier_version":"","isvalid":1,"_id":321586,"program_name":"xcs-dubbo-caishen-consumer-2","service_identifier":"xcs-dubbo-caishen-consumer-2","add_time":1662625402,"up_time":1662625402,"__v":0},"domain":"capital.hexin.cn","basepath":"/caishen_fund","case_list":[]}}
 */


/**
 *  请求接口地址代码：
 *
 */
const val POLL_FUND_ASSETS_INIT_STATUS = "4000:/caishen_fund/common/fund_cal_polling";



/**
 * 生成的请求数据类代码
 */
@Keep
data class FundInitPollReq(
    @SerializedName("fundid")
    val fundid: String,
    @SerializedName("page_id")
    val page_id: String,
    @SerializedName("account_type")
    val accountType: String = "fund",
    @SerializedName("id")
    val id: String = "",
    @SerializedName("userid")
    var userid: String = "",
    @SerializedName("polling_times")
    var pollingTimes:String? = ""
) : BaseReq()


/**
 * 生成的响应数据类代码
 */
@Keep
data class FundPollResp(
    @SerializedName("ex_data")
    val exData: List<ExData>? = listOf()
) : BaseT()

/**
 *  // 0 计算成功  1计算中 -1计算失败
 * @property fundid
 * @property status
 */
@Keep
data class ExData(
    @SerializedName("fundid")
    val fundid: String? = "",
    @SerializedName("status")
    val status: String? = ""
)

/**
 * 请求接口API
 */
interface FundInitPoll {


    /**
     * 基金计算轮循接口
     * <a href="http://software.myhexin.com/yapi/project/1526/interface/api/38556">...</a>
     */
    @API(POLL_FUND_ASSETS_INIT_STATUS)
    @POST(RetrofitManager.RETROFIT_PASS_THROUGH_URL)
    @Encrypt(encryptType = AES, keyType = PROTOCOL_KEY)
    fun  pollFundAssetsInitStatus(@Body  params:FundInitPollReq):Observable<FundPollResp>;
}