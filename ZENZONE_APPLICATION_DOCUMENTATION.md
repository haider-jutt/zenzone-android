# ZenZone Music Application - Developer Documentation

## Table of Contents
1. [Application Overview](#application-overview)
2. [Architecture](#architecture)
3. [Main Components](#main-components)
4. [API Reference](#api-reference)
5. [Hue Light Management](#hue-light-management)
6. [Music Subscription System](#music-subscription-system)
7. [Key Features](#key-features)
8. [Data Models](#data-models)
9. [Navigation Flow](#navigation-flow)

---

## Application Overview

**ZenZone** is an immersive meditation and music streaming Android application that provides:
- **Public Music Library**: Free meditation music and soundscapes for all users
- **VIP/Premium Music**: Subscription-based premium content (Premium & Infinite tiers)
- **Custom Studio Music**: Personalized music designed specifically for individual users by studio artists
- **Philips Hue Integration**: Dynamic light color synchronization with music playback
- **Multi-language Support**: English and French language support for all content

### Application Type
- **Platform**: Android (Kotlin)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Base URL**: Configured in `RemoteDataSource.kt`
- **Package**: `com.zenimmersive.android`

---

## Architecture

### Technology Stack
- **Language**: Kotlin
- **UI**: ViewBinding, Fragments
- **Networking**: Retrofit 2, OkHttp
- **Media**: ExoPlayer (Media3)
- **Image Loading**: Glide
- **Async Operations**: Kotlin Coroutines
- **Database**: Room (for local caching)
- **Firebase**: FCM for push notifications
- **In-App Purchases**: Google Play Billing

### Project Structure
```
com.zenimmersive.android/
├── adapter/              # RecyclerView adapters
├── apiresponsemodel/     # API response data models
├── base/                 # Base classes (Activity, Fragment, Repository, etc.)
├── helper/               # Utility classes and constants
├── hue/                  # Philips Hue integration
├── model/                # Local data models
├── repository/           # Data repositories
├── ui/                   # UI components (Activities & Fragments)
├── viewmodel/            # ViewModels
└── audiomuxer/          # Audio mixing utilities
```

---

## Main Components

### DashboardActivity
**File**: `app/src/main/java/com/zenimmersive/android/ui/DashboardActivity.kt`

The main activity and navigation hub of the application.

#### Key Responsibilities:
1. **Bottom Navigation Management**: 5 main sections
   - Home (Index 0)
   - Favorites (Index 1)
   - Player (Index 2)
   - VIP Music/Downloads (Index 3)
   - Settings (Index 4)

2. **Fragment Management**: 
   - Manages fragment backstack
   - Handles fragment transitions with animated bottom indicator
   - Maintains references to active PlayerFragment and HomeFragment

3. **Push Notifications**:
   - Handles deep links from Firebase notifications
   - Direct navigation to specific songs or albums via notification payload

4. **Permission Management**:
   - Requests POST_NOTIFICATIONS permission (Android 13+)

5. **FCM Token Management**:
   - Retrieves and updates device token for push notifications

#### Navigation Structure:
```kotlin
// Bottom Navigation Items
navHome      -> HomeFragment (Root, no backstack)
navHeart     -> FavoritesFragment (with backstack)
navPlayer    -> PlayerFragment (with backstack)
navDownload  -> VipMusicFragment (with backstack)
navUser      -> SettingFragment (with backstack)
```

#### Key Methods:

**setupViews()**
- Initializes bottom navigation
- Sets up navigation indicator animation
- Registers backstack change listener

**showPage(index: Int)**
- Handles fragment replacement based on page index
- Manages backstack for each section

**displayPlayerPage(bundle: Bundle)**
- Opens player screen with music data
- Shows player icon in bottom navigation

**displayPurchasePage(bundle: Bundle)**
- Shows subscription/purchase bottom sheet
- Handles purchase callbacks

---

## API Reference

### Base Configuration
- **Base URL**: Set in `RemoteDataSource.kt`
- **Authentication**: All APIs (except register/login) require:
  - `userId`: User ID
  - `userToken`: Authentication token

### API Endpoints

#### 1. Authentication APIs

##### Register
```
POST /api/register
```
**Parameters:**
```json
{
  "userName": "string",
  "email": "string",
  "password": "string",
  "lan": "en" // or "fr"
}
```
**Sample Response:**
```json
{
  "status": 1,
  "message": "Registration successful. Please verify OTP.",
  "result": {
    "userId": 123,
    "userToken": "abc123token",
    "email": "user@example.com"
  }
}
```

##### Verify OTP
```
POST /api/verifyOtp
```
**Parameters:**
```json
{
  "userId": 123,
  "userToken": "abc123token",
  "otp": "123456"
}
```

##### Social Login/Register
```
POST /api/socialMediaRegisterLogin
```
**Parameters:**
```json
{
  "email": "string",
  "registrationType": "google|facebook|apple",
  "socialToken": "string",
  "socialId": "string",
  "userName": "string",
  "lan": "en"
}
```

##### Login
```
POST /api/login
```
**Parameters:**
```json
{
  "email": "string",
  "password": "string",
  "lan": "en"
}
```
**Sample Response:**
```json
{
  "status": 1,
  "message": "Login successful",
  "result": {
    "userId": 123,
    "userToken": "abc123token",
    "userName": "John Doe",
    "email": "user@example.com",
    "userSubscriptionType": 1,
    "country": "US",
    "birthdate": "1990-01-01",
    "gender": 1
  }
}
```

##### Forgot Password
```
POST /api/forgotPassword
```
**Parameters:**
```json
{
  "email": "string",
  "lan": "en"
}
```

---

#### 2. User Management APIs

##### Get User Profile
```
GET /api/getUserProfile
```
**Query Parameters:**
- `userId`: int
- `userToken`: string

**Sample Response:**
```json
{
  "status": 1,
  "message": "Success",
  "result": {
    "userId": 123,
    "userName": "John Doe",
    "email": "user@example.com",
    "userSubscriptionType": 2,
    "country": "US",
    "birthdate": "1990-01-01",
    "gender": 1,
    "age": 33,
    "expireTime": "2024-12-31 23:59:59",
    "purchasedPackId": "premium_model_monthly",
    "purchasedPackType": "subscription",
    "purchaseTime": "2024-01-01 00:00:00",
    "purchaseToken": "purchase_token_here"
  }
}
```

##### Update Profile
```
POST /api/updateProfile
```
**Parameters:**
```json
{
  "userId": 123,
  "userToken": "string",
  "userName": "John Doe",
  "birthdate": "1990-01-01",
  "email": "user@example.com",
  "gender": 1,
  "country": "US"
}
```

##### Change Password
```
POST /api/changePassword
```
**Parameters:**
```json
{
  "userToken": "string",
  "newPassword": "string"
}
```

##### Logout
```
POST /api/logout
```
**Parameters:**
```json
{
  "userId": 123,
  "userToken": "string"
}
```

##### Delete Account
```
POST /api/deleteAccount
```
**Query Parameters:**
- `userId`: int
- `userToken`: string

---

#### 3. Music Content APIs

##### Get Home Data
```
GET /api/getHome
```
**Query Parameters:**
- `userId`: int
- `userToken`: string

**Sample Response:**
```json
{
  "status": 1,
  "message": "Success",
  "orderData": [
    "popular",
    "recentlyPlay",
    "meditationType",
    "albums"
  ],
  "result": {
    "popular": [
      {
        "songId": 1,
        "songName": "Ocean Waves",
        "songNameFrench": "Vagues de l'océan",
        "audioFileMusic": "https://example.com/audio/ocean_waves.mp3",
        "audioFileMusicFrench": "https://example.com/audio/ocean_waves_fr.mp3",
        "audioFileNarrator": "https://example.com/narrator/ocean_waves_narrator.mp3",
        "audioFileNarratorFrench": "https://example.com/narrator/ocean_waves_narrator_fr.mp3",
        "videoFileStream": "https://example.com/video/ocean_waves.mp4",
        "videoFileStreamFrench": "https://example.com/video/ocean_waves_fr.mp4",
        "narratorMusicVideoFile": "https://example.com/video/ocean_waves_narrator.mp4",
        "narratorMusicVideoFileFrench": "https://example.com/video/ocean_waves_narrator_fr.mp4",
        "backgroundImageVerticle": "https://example.com/images/ocean_waves.jpg",
        "backgroundImageVerticleFrench": "https://example.com/images/ocean_waves_fr.jpg",
        "hueColorTime": "https://example.com/hue/ocean_waves_colors.txt",
        "hueColorTimeFrench": "https://example.com/hue/ocean_waves_colors_fr.txt",
        "lyrics": "https://example.com/lyrics/ocean_waves.lrc",
        "lyricsFrench": "https://example.com/lyrics/ocean_waves_fr.lrc",
        "guidedFile": "https://example.com/guided/ocean_waves.txt",
        "guidedFileFrench": "https://example.com/guided/ocean_waves_fr.txt",
        "length": "300",
        "isGuided": 1,
        "isFGuided": 1,
        "isPaid": 0,
        "isVIP": 0,
        "isPurchased": 0,
        "isFavorite": 1,
        "isNew": 1,
        "cost": "2.99",
        "productId": "zen_ocean_waves",
        "previewLength": "30",
        "musicPackType": 1,
        "category": [
          {
            "categoryId": 1,
            "categoryName": "Nature",
            "categoryNameFrench": "Nature"
          }
        ],
        "artist": [
          {
            "artistId": 1,
            "artistName": "ZenZone Studio",
            "artistNameFrench": "Studio ZenZone"
          }
        ],
        "tag": [
          {
            "tagId": 1,
            "tagName": "Relaxation",
            "tagNameFrench": "Relaxation"
          }
        ]
      }
    ],
    "recentlyPlay": [],
    "meditationType": [
      {
        "tagId": 1,
        "tagName": "Relaxation",
        "tagNameFrench": "Relaxation",
        "tagImage": "https://example.com/tags/relaxation.jpg"
      }
    ],
    "albums": [
      {
        "albumId": 1,
        "albumName": "Beach Meditation",
        "albumNameFrench": "Méditation à la plage",
        "description": "Peaceful beach sounds",
        "descriptionFrench": "Sons paisibles de la plage",
        "smallBackgroundImgAlbum": "https://example.com/albums/beach_small.jpg",
        "smallBackgroundImgAlbumFrench": "https://example.com/albums/beach_small_fr.jpg",
        "bigBackgroundImgAlbum": "https://example.com/albums/beach_big.jpg",
        "bigBackgroundImgAlbumFrench": "https://example.com/albums/beach_big_fr.jpg",
        "totalSong": 5,
        "songAlbums": [],
        "subAlbums": [],
        "tagArray": []
      }
    ]
  }
}
```

##### Get Album List
```
GET /api/getAlbumList
```
**Query Parameters:**
- `userId`: int
- `userToken`: string

##### Get Album Details
```
GET /api/getAlbumDetail
```
**Query Parameters:**
- `userId`: int
- `userToken`: string
- `albumId`: int

**Sample Response:**
```json
{
  "status": 1,
  "message": "Success",
  "result": {
    "albumId": 1,
    "albumName": "Beach Meditation",
    "albumNameFrench": "Méditation à la plage",
    "description": "Peaceful beach sounds",
    "descriptionFrench": "Sons paisibles de la plage",
    "smallBackgroundImgAlbum": "https://example.com/albums/beach_small.jpg",
    "bigBackgroundImgAlbum": "https://example.com/albums/beach_big.jpg",
    "totalSong": 5,
    "songAlbums": [
      {
        "songId": 1,
        "songName": "Ocean Waves",
        "length": "300",
        "isPaid": 0,
        "isPurchased": 1,
        "isFavorite": 1
      }
    ],
    "subAlbums": []
  }
}
```

##### Get Song Details
```
GET /api/getSong
```
**Query Parameters:**
- `userId`: int
- `userToken`: string
- `songId`: int

**Sample Response:**
```json
{
  "status": 1,
  "message": "Success",
  "result": {
    "songId": 1,
    "songName": "Ocean Waves",
    "audioFileMusic": "https://example.com/audio/ocean_waves.mp3",
    "videoFileStream": "https://example.com/video/ocean_waves.mp4",
    "backgroundImageVerticle": "https://example.com/images/ocean_waves.jpg",
    "hueColorTime": "https://example.com/hue/ocean_waves_colors.txt",
    "lyrics": "https://example.com/lyrics/ocean_waves.lrc",
    "length": "300",
    "isGuided": 1,
    "isPaid": 0,
    "isPurchased": 1,
    "isFavorite": 1,
    "cost": "2.99",
    "musicPackType": 1
  }
}
```

##### Get VIP Songs
```
GET /api/getVIPUserSongList
```
**Query Parameters:**
- `userId`: int
- `userToken`: string

**Description**: Returns personalized/custom music created specifically for the authenticated user by studio artists.

**Sample Response:**
```json
{
  "status": 1,
  "message": "Success",
  "result": [
    {
      "songId": 101,
      "songName": "John's Personal Meditation",
      "songNameFrench": "Méditation personnelle de John",
      "audioFileMusic": "https://example.com/vip/john_meditation.mp3",
      "length": "600",
      "isPaid": 1,
      "isVIP": 1,
      "isPurchased": 0,
      "cost": "19.99",
      "musicPackType": 3
    }
  ]
}
```

---

#### 4. Search & Filter APIs

##### Search Songs
```
GET /api/searchSong
```
**Query Parameters:**
- `userId`: int
- `userToken`: string
- `searchKeyword`: string

**Sample Response:**
```json
{
  "status": 1,
  "message": "Success",
  "result": [
    {
      "songId": 1,
      "songName": "Ocean Waves",
      "length": "300",
      "isPaid": 0,
      "isFavorite": 1
    }
  ]
}
```

##### Filter Songs
```
GET /api/filter
```
**Query Parameters:**
- `userId`: int
- `userToken`: string
- `tagId`: string (comma-separated)
- `fromDuration`: string (in seconds)
- `toDuration`: string (in seconds)

**Example**: `/api/filter?userId=123&userToken=abc&tagId=1,2,3&fromDuration=60&toDuration=600`

**Sample Response:**
```json
{
  "status": 1,
  "message": "Success",
  "result": [
    {
      "songId": 1,
      "songName": "Ocean Waves",
      "length": "300",
      "tag": [
        {"tagId": 1, "tagName": "Relaxation"}
      ]
    }
  ]
}
```

##### Get Tags
```
GET /api/getTagList
```
**Query Parameters:**
- `userId`: int
- `userToken`: string

**Sample Response:**
```json
{
  "status": 1,
  "message": "Success",
  "result": [
    {
      "tagId": 1,
      "tagName": "Relaxation",
      "tagNameFrench": "Relaxation",
      "tagImage": "https://example.com/tags/relaxation.jpg"
    }
  ]
}
```

##### Get Tag Details
```
GET /api/getTagDetails
```
**Query Parameters:**
- `userId`: int
- `userToken`: string
- `tagId`: int

---

#### 5. Favorites APIs

##### Add/Remove Favorite
```
POST /api/favouriteSong
```
**Parameters:**
```json
{
  "userId": 123,
  "userToken": "string",
  "songId": 1,
  "isFavorite": 1
}
```
**Notes:**
- `isFavorite`: 1 = Add to favorites, 0 = Remove from favorites

**Sample Response:**
```json
{
  "status": 1,
  "message": "Song added to favorites",
  "result": {}
}
```

##### Get Favorite Songs
```
GET /api/getFavoriteSongs
```
**Query Parameters:**
- `userId`: int
- `userToken`: string

**Sample Response:**
```json
{
  "status": 1,
  "message": "Success",
  "result": [
    {
      "songId": 1,
      "songName": "Ocean Waves",
      "isFavorite": 1
    }
  ]
}
```

---

#### 6. Purchase & Subscription APIs

##### Handle User Purchase
```
POST /api/handleUserPurchase
```
**Parameters:**
```json
{
  "userId": 123,
  "userToken": "string",
  "purchasePackId": "premium_model_monthly",
  "purchasedPackType": 2,
  "purchaseTime": "2024-01-01 00:00:00",
  "purchaseDuration": 30,
  "musicPackId": 1,
  "purchaseToken": "google_play_purchase_token"
}
```
**Notes:**
- `purchasedPackType`: 1=Free, 2=Premium, 3=Infinite
- `purchaseDuration`: Duration in days

##### Verify In-App Purchase
```
POST /api/verifyInAppUserPuchase
```
**Parameters:**
```json
{
  "userId": 123,
  "userToken": "string"
}
```

##### Get Order List
```
GET /api/getOrderList
```
**Query Parameters:**
- `userId`: int
- `userToken`: string

**Sample Response:**
```json
{
  "status": 1,
  "message": "Success",
  "result": [
    {
      "orderId": 1,
      "musicPackName": "Ocean Waves",
      "purchasePackId": "zen_ocean_waves",
      "purchaseTime": "2024-01-01 00:00:00",
      "cost": "2.99"
    }
  ]
}
```

---

#### 7. Other APIs

##### Update Device Token
```
POST /api/deviceTokenUpdate
```
**Parameters:**
```json
{
  "userId": 123,
  "deviceToken": "firebase_fcm_token"
}
```

##### Get FAQ
```
GET /api/getFaq
```
**Query Parameters:**
- `userId`: int
- `userToken`: string
- `lan`: string (en/fr)

##### Get Static Pages
```
GET /api/getStaticPages
```
**Query Parameters:**
- `userId`: int
- `userToken`: string
- `staticId`: int

**Notes:**
- staticId values (typically):
  - 1 = Terms & Conditions
  - 2 = Privacy Policy
  - 3 = About Us

##### Get Quote
```
GET /api/getQuote
```
**No parameters required**

**Sample Response:**
```json
{
  "status": 1,
  "message": "Success",
  "result": {
    "quote": "Peace comes from within. Do not seek it without.",
    "author": "Buddha"
  }
}
```

---

## Hue Light Management

### Overview
ZenZone integrates with **Philips Hue smart lighting** to create an immersive meditation experience by synchronizing light colors with music playback.

### Architecture

#### Components:
1. **HueLightManager**: Main manager for Hue operations
2. **HueLightClient**: Handles API calls to Hue Bridge
3. **HueColorManager**: Manages color synchronization with music
4. **HueTokenClient**: Handles OAuth authentication with Hue
5. **ColorConverter**: Converts between RGB and XY color spaces

### Hue Bridge Connection

#### Discovery & Setup:
1. User discovers local Hue Bridge on network
2. User presses bridge button for pairing
3. App creates API key (stored in `KeyStorage`)
4. Fetches available lights and rooms

#### Hue API Endpoints:
```
Base URL: https://{bridge_ip}/clip/v2/resource/
Authorization: hue-application-key: {api_key}
```

**Get Lights:**
```
GET /light
Response: List of Light objects with current state
```

**Update Light State:**
```
PUT /light/{light_id}
Body: {
  "on": {"on": true},
  "color": {
    "xy": {"x": 0.4, "y": 0.5}
  },
  "dimming": {"brightness": 80.0},
  "dynamics": {"duration": 1000}
}
```

### Dynamic Color Files

Each music track has an associated Hue color file (`hueColorTime` field in AlbumMusic).

#### File Format:
Plain text file with comma-separated values:
```
start_time,end_time,x,y,brightness
00:00,00:05,0.561,0.348,255
00:05,00:08,0.352,0.508,255
00:08,00:12,0.212,0.121,255
00:12,00:20,0.417,0.504,255
```

**Format Details:**
- `start_time`, `end_time`: MM:SS format
- `x`, `y`: CIE 1931 color space coordinates (0.0 - 1.0)
- `brightness`: 0-255 (converted to percentage for Hue API)

#### Color Synchronization Process:

1. **Load Color Data**:
```kotlin
HueColorManager.loadColorData(context, musicPack)
```
- Downloads color file from server or loads from cache
- Parses CSV format into `ColorInterval` objects
- Sorts intervals by start time for binary search

2. **Real-time Color Updates**:
```kotlin
HueColorManager.changeColor(hueLightManager, playerPositionMillis)
```
- Called periodically during music playback
- Uses binary search to find matching color interval
- Updates only lights marked for "system use"
- Applies smooth transitions (1000ms duration)

3. **Color Space Conversion**:
```kotlin
ColorConverter.rgbToXY(rgbColor): FloatArray
ColorConverter.xyToRGB(x, y, brightness): Int
```

### User Light Selection

Users can select which lights participate in music synchronization:
- Select lights by room or individually
- Toggle lights on/off for app control
- Brightness control (0-100%)
- Lights restore to previous state after playback

---

## Music Subscription System

### Subscription Tiers

#### 1. Free (musicPackType = 1)
- Access to free music library
- Limited content
- Ads may be shown
- `userSubscriptionType = 1`

#### 2. Premium (musicPackType = 2)
- Access to premium music library
- No ads
- Monthly/Yearly subscription options
  - `premium_model_monthly`
  - `premium_model_yearly`
- `userSubscriptionType = 2`

#### 3. Infinite (musicPackType = 3)
- Full access to all content including VIP
- Custom music creation by studio artists
- Highest tier subscription
- Monthly/Yearly subscription options
  - `infinite_model_monthly`
  - `infinite_model_yearly`
- `userSubscriptionType = 3`

### Purchase Logic

#### Access Control:
```kotlin
fun shouldShowPaidStatus(context: Context, songData: AlbumMusic): Boolean
```

**Logic:**
1. If song is free (`isPaid = 0`), allow access
2. If song is already purchased (`isPurchased = 1`), allow access
3. Compare user's subscription tier with song's required tier
4. If `userSubscriptionType >= musicPackType`, allow access
5. Check local purchase cache for individual song purchases
6. Otherwise, show purchase dialog

#### Purchase Types:

**Subscription Purchase:**
- Managed via Google Play Billing
- Provides access to entire tier
- Updates `userSubscriptionType` in user profile
- Has expiration date

**Individual Song Purchase:**
- One-time payment for specific song
- Stored in local preferences and server
- No expiration
- Accessed via `KeyStorage.storePurchasePackId(songId)`

### VIP Music System

**Custom Studio Music:**
- Personalized meditation/music created for specific user
- Commissioned through special requests
- Highest cost tier
- Retrieved via `/api/getVIPUserSongList`
- Only visible to the user it was created for

**Use Cases:**
1. Corporate wellness programs
2. Therapy/clinical use
3. High-value individual clients

---

## Key Features

### 1. Music Player System

**PlayerManager** (`ui/player/PlayerManager.kt`)
- **Triple Audio System**:
  - Music Audio (background music)
  - Narrator Audio (guided meditation voice)
  - Video Stream (visual meditation)
- **Audio Mixing**: Combines music + narrator with volume control
- **ExoPlayer Integration**: Media3 library
- **Download Support**: Offline playback
- **Preview Mode**: 30-second preview for paid content

**Player Controls:**
- Play/Pause
- Previous/Next track
- Seek
- Shuffle/Repeat
- Volume control for music and narrator independently
- Playback speed control

### 2. Dynamic Content Loading

**HomeFragment** loads content in dynamic order:
- Server provides `orderData` array
- Frontend renders sections in specified order
- Sections: `popular`, `recentlyPlay`, `meditationType`, `albums`
- Allows A/B testing and personalization

### 3. Multi-Language Support

**Supported Languages:**
- English (`en`)
- French (`fr`)

**Implementation:**
- Every content field has English and French versions
- User selects language in settings
- Stored in `KeyStorage.APP_SELECTED_LANGUAGE`
- Falls back to English if French version unavailable

**Naming Convention:**
```kotlin
// Regular fields
songName
audioFileMusic
backgroundImageVerticle

// French equivalents
songNameFrench
audioFileMusicFrench
backgroundImageVerticleFrench
```

### 4. Favorites System

- Mark songs as favorites
- Dedicated Favorites tab
- Synced to server
- Quick access to preferred content

### 5. Search & Filter

**Search:**
- Search by song name
- Search across all content
- Real-time results

**Filter:**
- Filter by meditation type (tags)
- Filter by duration range
- Multiple tag selection
- Combination filters

### 6. Download Management

**MusicPackDownloader** (`helper/MusicPackDownloader.kt`)
- Downloads music files for offline use
- Downloads Hue color files
- Downloads lyric files
- Caches images via Glide
- Progress tracking
- Resume support

### 7. Lyrics Display

**LyricView** (`helper/CustomLyricView.java`)
- LRC format support
- Time-synchronized lyrics
- Auto-scroll with playback
- Smooth animations

### 8. Firebase Push Notifications

**Deep Linking:**
- Notification with `notification_song_id` → Opens player with that song
- Notification with `notification_album_id` → Opens album/playlist

**FirebaseMessagingService:**
- Handles incoming messages
- Creates notifications
- Manages click actions

---

## Data Models

### AlbumMusic (Song Data)
```kotlin
data class AlbumMusic(
    var songId: Int?,
    var songName: String?,
    var songNameFrench: String?,
    var audioFileMusic: String?,                 // Music audio URL
    var audioFileMusicFrench: String?,
    var audioFileNarrator: String?,              // Narrator audio URL
    var audioFileNarratorFrench: String?,
    var videoFileStream: String?,                // Video URL
    var videoFileStreamFrench: String?,
    var narratorMusicVideoFile: String?,         // Video with narrator
    var narratorMusicVideoFileFrench: String?,
    var backgroundImageVerticle: String?,        // Background image
    var hueColorTime: String?,                   // Hue color file URL
    var hueColorTimeFrench: String?,
    var lyrics: String?,                         // Lyrics file URL
    var lyricsFrench: String?,
    var guidedFile: String?,
    var guidedFileFrench: String?,
    var length: String?,                         // Duration in seconds
    var isGuided: Int?,                          // Has narrator
    var isFGuided: Int?,                         // Has French narrator
    var isPaid: Int?,                            // 0=Free, 1=Paid
    var isVIP: Int?,                             // 0=Public, 1=VIP only
    var isPurchased: Int?,                       // 0=Not purchased, 1=Purchased
    var isFavorite: Int?,                        // 0=Not favorite, 1=Favorite
    var isNew: Int?,                             // 0=Old, 1=New
    var cost: String?,                           // Price (USD or EUR)
    var productId: String?,                      // Google Play product ID
    var previewLength: String?,                  // Preview duration (seconds)
    var musicPackType: Int?,                     // 1=Free, 2=Premium, 3=Infinite
    var category: List<Category?>?,
    var artist: List<Artist>?,
    var tag: List<Tag>?,
    var lastTimeMusicPosition: Long?             // Resume position
)
```

### User Data
```kotlin
data class UserDetailsResModel(
    var userId: Int?,
    var userName: String?,
    var email: String?,
    var userToken: String?,
    var userSubscriptionType: Int?,              // 1=Free, 2=Premium, 3=Infinite
    var country: String?,
    var birthdate: String?,
    var gender: Int?,                            // 1=Male, 2=Female, 3=Other
    var age: Int?,
    var expireTime: String?,                     // Subscription expiry
    var purchasedPackId: String?,                // Current subscription product ID
    var purchasedPackType: String?,
    var purchaseTime: String?,
    var purchaseToken: String?                   // Google Play purchase token
)
```

### Hue Light Models
```kotlin
data class Light(
    var id: String?,
    var metadata: Metadata?,                     // name, archetype
    var on: On?,                                 // on/off state
    var dimming: Dimming?,                       // brightness
    var color: Color?,                           // XY color
    var dynamics: Dynamics?,                     // transition settings
    var systemUseCase: Boolean?                  // Selected for app control
)

data class Color(
    var xy: Xy?,                                 // CIE 1931 color space
    var gamut: Gamut?
)

data class Xy(
    var x: Double?,                              // 0.0 - 1.0
    var y: Double?                               // 0.0 - 1.0
)
```

---

## Navigation Flow

### App Launch Flow:
```
SplashActivity
    ↓
LoginActivity (if not authenticated)
    ↓
DashboardActivity
    ↓
HomeFragment (default)
```

### Main Navigation:
```
DashboardActivity (Bottom Navigation)
├── HomeFragment (0)
│   ├── Search
│   ├── Album Details → HomePlaylistFragment
│   │   └── Song Selection → PlayerFragment
│   ├── Tag Details → TagSongsFragment
│   └── View All → ViewAllAlbumsFragment
├── FavoritesFragment (1)
│   └── Song Selection → PlayerFragment
├── PlayerFragment (2)
│   ├── Hue Light Control → ColorFragment
│   └── Purchase Dialog → SubscriptionBSFragment
├── VipMusicFragment (3)
│   └── VIP Song Selection → PlayerFragment
└── SettingFragment (4)
    ├── Profile
    ├── Hue Settings → HueLightFragment
    ├── Language Selection
    ├── FAQ
    ├── Static Pages
    └── Logout
```

### Deep Link Flow:
```
Push Notification
    ↓
DashboardActivity.onCreate()
    ↓
Check intent extras:
    - notification_song_id → PlayerFragment
    - notification_album_id → HomePlaylistFragment
```

---

## Implementation Notes

### Local Data Storage

**SharedPreferences (KeyStorage):**
- User authentication (userId, userToken)
- User subscription type
- Selected language
- Hue bridge configuration
- Cached music playback position
- Purchased music pack IDs

**Room Database:**
- Log files
- Offline music metadata

**File System:**
- Downloaded music files
- Downloaded Hue color files
- Downloaded lyric files
- Image cache (via Glide)

### Error Handling

**API Errors:**
```json
{
  "status": 0,
  "message": "Error message here",
  "result": null
}
```

**Common Status Codes:**
- `status: 1` = Success
- `status: 0` = Error/Failure

### Security Considerations

1. **Authentication**: All requests include userToken
2. **HTTPS**: All API calls over HTTPS
3. **Local Storage**: Sensitive data in SharedPreferences
4. **Purchase Verification**: Server-side validation of Google Play purchases
5. **Hue API Key**: Stored securely, never transmitted to backend

---

## Testing APIs

### Sample Test Flow:

1. **Register User:**
```bash
POST /api/register
{
  "userName": "Test User",
  "email": "test@example.com",
  "password": "password123",
  "lan": "en"
}
```

2. **Verify OTP:**
```bash
POST /api/verifyOtp
{
  "userId": <from_register_response>,
  "userToken": <from_register_response>,
  "otp": "123456"
}
```

3. **Get Home Data:**
```bash
GET /api/getHome?userId=123&userToken=abc123token
```

4. **Play Song:**
- Use audioFileMusic URL from song object
- Load Hue colors from hueColorTime URL
- Display lyrics from lyrics URL

5. **Add to Favorites:**
```bash
POST /api/favouriteSong
{
  "userId": 123,
  "userToken": "abc123token",
  "songId": 1,
  "isFavorite": 1
}
```

---

## Additional Resources

### Key Configuration Files:
- `Constants.kt`: API endpoints and constants
- `ServerApi.kt`: Retrofit API interface
- `build.gradle.kts`: Dependencies and build configuration
- `google-services.json`: Firebase configuration

### Important Helper Classes:
- `KeyStorage`: SharedPreferences wrapper
- `CommonUtils`: Utility functions
- `LogSystem`: Logging framework
- `PlayerManager`: Music playback singleton
- `HueLightManager`: Hue integration singleton

---

## Contact & Support

For API issues, backend questions, or feature requests, contact the backend development team.

For Hue integration issues, refer to [Philips Hue API Documentation](https://developers.meethue.com/).

---

**Document Version**: 1.0
**Last Updated**: November 2025
**App Package**: com.zenimmersive.android

