package com.zenimmersive.android.base

import com.zenimmersive.android.helper.Constants.ADD_REMOVE_FAVORITE_API
import com.zenimmersive.android.helper.Constants.CHANGE_PASSWORD_API
import com.zenimmersive.android.helper.Constants.DELETE_ACCOUNT_API
import com.zenimmersive.android.helper.Constants.DEVICE_TOKEN_UPDATE_API
import com.zenimmersive.android.helper.Constants.FILTER_SONGS_API
import com.zenimmersive.android.helper.Constants.FORGOT_PASSWORD
import com.zenimmersive.android.helper.Constants.GET_ALBUM_DETAIL_API
import com.zenimmersive.android.helper.Constants.GET_ALL_ALBUM_API
import com.zenimmersive.android.helper.Constants.GET_FAQ_API
import com.zenimmersive.android.helper.Constants.GET_FAVORITE_SONGS_API
import com.zenimmersive.android.helper.Constants.GET_HOME_API
import com.zenimmersive.android.helper.Constants.GET_QUOTE_API
import com.zenimmersive.android.helper.Constants.GET_SEARCH_SONG_API
import com.zenimmersive.android.helper.Constants.GET_SONG_API
import com.zenimmersive.android.helper.Constants.GET_TAGS_SONG_API
import com.zenimmersive.android.helper.Constants.GET_TAG_LIST_API
import com.zenimmersive.android.helper.Constants.GET_USER_PROFILE_API
import com.zenimmersive.android.helper.Constants.HANDLE_USER_PURCHASE
import com.zenimmersive.android.helper.Constants.LOGIN_API
import com.zenimmersive.android.helper.Constants.LOGOUT_API
import com.zenimmersive.android.helper.Constants.Order_List_API
import com.zenimmersive.android.helper.Constants.REGISTER_API
import com.zenimmersive.android.helper.Constants.SOCIAL_REGISTER_LOGIN_API
import com.zenimmersive.android.helper.Constants.STATIC_PAGES_API
import com.zenimmersive.android.helper.Constants.UPDATE_PROFILE_API
import com.zenimmersive.android.helper.Constants.VERIFY_OTP_API
import com.zenimmersive.android.helper.Constants.VIP_SONGS_API
import com.zenimmersive.android.helper.Constants.SUBSCRIPTION_PURCHASE_API
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ServerApi {

    @POST(REGISTER_API)
    @FormUrlEncoded
    fun register(
        @Field("userName") userName: String,
        @Field("email") email: String,
        @Field("password") password: String,
        @Query("lan") lan: String,
    ): Call<ResponseBody>

    @POST(VERIFY_OTP_API)
    @FormUrlEncoded
    fun verifyOtp(
        @Field("userId") userId: Int,
        @Field("userToken") userToken: String,
        @Field("otp") otp: String,
    ): Call<ResponseBody>

    @POST(SOCIAL_REGISTER_LOGIN_API)
    @FormUrlEncoded
    fun socialRegisterLogin(
        @Field("email") email: String,
        @Field("registrationType") registrationType: String,
        @Field("socialToken") socialToken: String,
        @Field("socialId") socialId: String,
        @Field("userName") userName: String,
        @Query("lan") lan: String,
    ): Call<ResponseBody>

    @POST(LOGIN_API)
    @FormUrlEncoded
    fun loginApi(
        @Field("email") email: String,
        @Field("password") password: String,
        @Query("lan") lan: String,
    ): Call<ResponseBody>

    @POST(FORGOT_PASSWORD)
    @FormUrlEncoded
    fun forgotPassword(
        @Field("email") email: String,
        @Query("lan") lan: String,
    ): Call<ResponseBody>

    @POST(CHANGE_PASSWORD_API)
    @FormUrlEncoded
    fun changePassword(
        @Field("userToken") userToken: String,
        @Field("newPassword") newPassword: String,
    ): Call<ResponseBody>

    @GET(GET_USER_PROFILE_API)
    fun getUserDetails(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
    ): Call<ResponseBody>

    @POST(UPDATE_PROFILE_API)
    @FormUrlEncoded
    fun updateProfileApi(
        @Field("userId") userId: Int,
        @Field("userToken") userToken: String,
        @Field("userName") userName: String,
        @Field("birthdate") birthdate: String,
        @Field("email") email: String,
        @Field("gender") gender : Int,
        @Field("country") country : String,
    ): Call<ResponseBody>

    @POST(LOGOUT_API)
    @FormUrlEncoded
    fun logout(
        @Field("userId") userId: Int,
        @Field("userToken") userToken: String,
    ): Call<ResponseBody>

    @GET(GET_FAQ_API)
    fun getFaq(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
        @Query("lan") lan: String,
    ): Call<ResponseBody>

    @GET(STATIC_PAGES_API)
    fun staticPages(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
        @Query("staticId") staticId: Int,
    ): Call<ResponseBody>

    @GET(GET_HOME_API)
    fun homeApi(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
    ): Call<ResponseBody>

    @GET(GET_ALL_ALBUM_API)
    fun getAllAlbums(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
    ): Call<ResponseBody>

    @GET(GET_ALBUM_DETAIL_API)
    fun getAlbumDetails(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
        @Query("albumId") albumId: Int,
    ): Call<ResponseBody>

    @GET(GET_SONG_API)
    fun getSongDetails(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
        @Query("songId") songId: Int,
    ): Call<ResponseBody>

    @GET(GET_TAGS_SONG_API)
    fun getTagSongApi(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
        @Query("tagId") tagId: Int,
    ): Call<ResponseBody>

    @GET(GET_TAG_LIST_API)
    fun getTagListApi(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
    ): Call<ResponseBody>

    @GET(GET_SEARCH_SONG_API)
    fun searchSongs(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
        @Query("searchKeyword") searchKeyword: String,
    ): Call<ResponseBody>

    @GET(FILTER_SONGS_API)
    fun filterSongs(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
        @Query("tagId") tagId: String,
        @Query("fromDuration") fromDuration: String,
        @Query("toDuration") toDuration: String,
    ): Call<ResponseBody>

    @POST(ADD_REMOVE_FAVORITE_API)
    @FormUrlEncoded
    fun addRemoveFavoriteApi(
        @Field("userId") userId: Int,
        @Field("userToken") userToken: String,
        @Field("songId") songId: Int,
        @Field("isFavorite") isFavorite: Int,
    ): Call<ResponseBody>

    @GET(GET_FAVORITE_SONGS_API)
    fun getFavoritesSongs(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
    ): Call<ResponseBody>

    @POST(DELETE_ACCOUNT_API)
    fun deleteAccountApi(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
    ): Call<ResponseBody>

    @GET(VIP_SONGS_API)
    fun getVipSongs(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
    ): Call<ResponseBody>

    @POST(DEVICE_TOKEN_UPDATE_API)
    @FormUrlEncoded
    fun deviceTokenUpdateApi(
        @Field("userId") userId: Int,
        @Field("deviceToken") deviceToken: String,
    ): Call<ResponseBody>

    @POST(HANDLE_USER_PURCHASE)
    @FormUrlEncoded
    fun handleUserPurchase(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
        @Field("purchasePackId") purchasedPackId: String,
        @Field("purchasedPackType") purchasedPackType: Int,
        @Field("purchaseTime") purchaseTime: String,
        @Field("purchaseDuration") purchasePackDuration: Int,
        @Field("musicPackId") songId: Int,
        @Field("purchaseToken") purchaseToken: String,
        @Field("cost") cost : String,
        @Field("orderId") orderId: String?=""
    ): Call<ResponseBody>

    @POST(SUBSCRIPTION_PURCHASE_API)
    @FormUrlEncoded
    fun verifyInAppUserPurchase(
        @Field("userId") userId: Int,
        @Field("userToken") userToken: String,

    ): Call<ResponseBody>

    @GET(Order_List_API)
    fun getOrdersList(
        @Query("userId") userId: Int,
        @Query("userToken") userToken: String,
    ): Call<ResponseBody>

    @GET(GET_QUOTE_API)
    fun getQuoteMessage(): Call<ResponseBody>
}