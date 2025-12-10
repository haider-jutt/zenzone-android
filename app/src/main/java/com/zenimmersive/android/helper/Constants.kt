package com.zenimmersive.android.helper

object Constants {
    const val ACCEPT_TERMS = "accept_terms"
    const val USER_DATA = "user_data"
    const val SELECTED_ALBUM_DATA = "selected_album_data"
    const val SELECTED_MEDITATION_DATA = "selected_meditation_data"
    const val ALBUM_ID = "album_id"
    const val ALBUM_IMAGE = "album_image"
    const val ALBUM_TITLE = "album_title"
    const val ALBUM_SESSION = "album_session"
    const val ALBUM_DETAILS = "album_details"
    const val SONG_ID = "song_id"
    const val PREVIEW_SEC_LENGTH = "preview_sec_length"
    const val IS_PLAY_PREVIEW = "is_play_preview"
    const val SONG_DATA = "song_data"
    const val TAG_ID = "tag_id"
    const val All_SONGS = "all_songs"
    const val NOTIFICATION_ALBUM_ID = "notification_album_id"
    const val NOTIFICATION_SONG_ID = "notification_song_id"


    // Apis End Points
    const val REGISTER_API = "api/register"
    const val SOCIAL_REGISTER_LOGIN_API = "api/socialMediaRegisterLogin"
    const val VERIFY_OTP_API = "api/verifyOtp"
    const val LOGIN_API = "api/login"
    const val FORGOT_PASSWORD = "api/forgotPassword"
    const val GET_USER_PROFILE_API = "api/getUserProfile"
    const val UPDATE_PROFILE_API = "api/updateProfile"
    const val LOGOUT_API = "api/logout"
    const val GET_FAQ_API = "api/getFaq"
    const val STATIC_PAGES_API = "api/getStaticPages"
    const val GET_HOME_API = "api/getHome"
    const val GET_ALL_ALBUM_API = "api/getAlbumList"
    const val GET_ALBUM_DETAIL_API = "api/getAlbumDetail"
    const val GET_SONG_API = "api/getSong"
    const val GET_TAGS_SONG_API = "api/getTagDetails"
    const val GET_TAG_LIST_API = "api/getTagList"
    const val GET_SEARCH_SONG_API = "api/searchSong"
    const val CHANGE_PASSWORD_API = "api/changePassword"
    const val FILTER_SONGS_API = "api/filter"
    const val ADD_REMOVE_FAVORITE_API = "api/favouriteSong"
    const val GET_FAVORITE_SONGS_API = "api/getFavoriteSongs"
    const val DELETE_ACCOUNT_API = "api/deleteAccount"
    const val VIP_SONGS_API = "api/getVIPUserSongList"
    const val DEVICE_TOKEN_UPDATE_API = "api/deviceTokenUpdate"
    const val HANDLE_USER_PURCHASE = "api/handleUserPurchase"
    const val Order_List_API = "api/getOrderList"
    const val GET_QUOTE_API = "api/getQuote"
    const val SUBSCRIPTION_PURCHASE_API = "api/verifyInAppUserPuchase"


    // for application language
    val languageMap : Map<String, String> = mapOf("English" to "en", "Français" to "fr", "French" to "fr")

}