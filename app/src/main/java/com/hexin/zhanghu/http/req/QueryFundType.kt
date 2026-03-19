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
 * {"errcode":0,"errmsg":"成功！","data":{"query_path":{"path":"/fund_common/v1/query_fund_type","params":[]},"protocol":"http","isArsenal":false,"serviceId":0,"serviceName":"","select_business":"","select_businessname":"","applicationid":"","applicationname":"","nocover":0,"arsenalUrl":"","edit_uid":0,"status":"done","type":"static","req_body_is_json_schema":true,"res_body_is_json_schema":true,"api_opened":false,"index":0,"tag":["接口测试-支持自动化","接口测试-支持巡检"],"operate_type":"query","cooperate_mode":"request-response","_id":440875,"code_list":[{"_id":"64b0bf66f45fae0109e3c245","code":"-2","value":"fail","desc":"请求异常"},{"_id":"64b0bf59f45fae0109e3c244","code":"0","value":"success","desc":"请求成功"}],"method":"GET","catid":390793,"title":"批量查询基金类型","path":"/fund_common/v1/query_fund_type","project_id":1526,"req_params":[],"res_body_type":"json","req_query":[{"is_constant":"0","required":"1","_id":"64c226a2979b6e01083e97b7","name":"userid","example":"123456","desc":"同花顺id"},{"is_constant":"0","required":"1","_id":"64c226a2979b6e01083e97b6","name":"fundcodes","query_type":"string","example":"008888,161725","desc":"基金代码，逗号分隔"}],"req_headers":[],"req_body_form":[],"res_body":"{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"object\",\"properties\":{\"error_code\":{\"type\":\"string\",\"mock\":{\"mock\":\"0\"},\"description\":\"状态码\"},\"error_msg\":{\"type\":\"string\",\"mock\":{\"mock\":\"success\"},\"description\":\"错误信息\"},\"ex_data\":{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\",\"description\":\"h5id\",\"mock\":{\"mock\":\"123\"},\"default\":\"1\"},\"list\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"code\":{\"type\":\"string\",\"mock\":{\"mock\":\"008888\"},\"description\":\"基金代码\"},\"name\":{\"type\":\"string\",\"mock\":{\"mock\":\"招商中证白酒\"},\"description\":\"基金名称\"},\"fund_type\":{\"type\":\"string\",\"description\":\"基金类型\",\"mock\":{\"mock\":\"1\"}}},\"description\":\"基金\",\"required\":[\"code\",\"name\",\"fund_type\"]},\"description\":\"基金查询结果列表\"}},\"required\":[\"id\",\"list\"],\"description\":\"返回数据\"}},\"required\":[\"ex_data\",\"error_code\",\"error_msg\"]}","markdown":"<span class=\"colour\" style=\"color:rgb(85, 85, 85)\">基金类型,对应fund01表 0-股票型；1-债券型；2-混合型；3-货币型；4-其他；6-基金型；7-保本型；8-REITs</span>","desc":"<p><span class=\"colour\" style=\"color:rgb(85, 85, 85)\">基金类型,对应fund01表 0-股票型；1-债券型；2-混合型；3-货币型；4-其他；6-基金型；7-保本型；8-REITs</span></p>\n","service_program_id":321586,"uid":757,"add_time":1690285652,"up_time":1690445474,"__v":0,"domain_path":[{"_id":"689a9667805a0a0100da8836","env":"test","domain":"testcapital.hexin.cn","path":"/caishen_fund"},{"_id":"689a9667805a0a0100da8835","env":"pre","domain":"pretestcapital.hexin.cn","path":"/caishen_fund"}],"username":"wudi","edit_username":"","service_program_data":{"service_identifier_version":"","isvalid":1,"_id":321586,"program_name":"xcs-dubbo-caishen-consumer-2","service_identifier":"xcs-dubbo-caishen-consumer-2","add_time":1662625402,"up_time":1662625402,"__v":0},"domain":"capital.hexin.cn","basepath":"/caishen_fund","case_list":[]}}
 */

/**
 *  请求接口地址代码：
 *
 */
const val QUERY_FUND_TYPE = "4000:/caishen_fund/fund_common/v1/query_fund_type";



/**
 * 生成的请求数据类代码
 */
@Keep
data class QueryFundTypeReq(
    /**
     * 同花顺id
     */
    @SerializedName("userid")
    val userid: String = "",
    /**
     * 基金代码，逗号分隔
     */
    @SerializedName("fundcodes")
    val fundcodes: String = ""
) : BaseReq()


/**
 * 生成的响应数据类代码
 */
@Keep
data class QueryFundTypeResp(
    /**
     * 基金查询结果列表
     */
    @SerializedName("list")
    var list: List<FundTypeData>? = emptyList()
) : BaseT()

@Keep
data class FundTypeData(
    /**
     * 基金代码
     */
    @SerializedName("code")
    var code: String? = "",
    /**
     * 基金名称
     */
    @SerializedName("name")
    var name: String? = "",
    /**
     * 基金类型
     */
    @SerializedName("fund_type")
    var fundType: String? = ""
)

/**
 * 请求接口API
 */
interface QueryFundType {
    /**
     * 基金类型,对应fund01表 0-股票型；1-债券型；2-混合型；3-货币型；4-其他；6-基金型；7-保本型；8-REITs
     *
     * 4000:/caishen_fund/fund_common/v1/query_fund_type
     * 批量查询基金类型
     * @param param QueryFundTypeReq
     * @return QueryFundTypeResp
     */
    @API(QUERY_FUND_TYPE)
    @POST(RetrofitManager.RETROFIT_PASS_THROUGH_URL)
    @Encrypt(encryptType = AES, keyType = PROTOCOL_KEY)
    fun queryFundType(@Body param: QueryFundTypeReq?): Observable<QueryFundTypeResp?>?
}