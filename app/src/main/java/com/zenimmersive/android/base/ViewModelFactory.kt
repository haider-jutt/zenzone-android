package com.zenimmersive.android.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenimmersive.android.repository.BlankRepository
import com.zenimmersive.android.repository.ChangePasswordRepository
import com.zenimmersive.android.repository.DashboardRepository
import com.zenimmersive.android.repository.UserProfileRepository
import com.zenimmersive.android.repository.FavoritesRepository
import com.zenimmersive.android.repository.ForgotPasswordRepository
import com.zenimmersive.android.repository.HomePlaylistRepository
import com.zenimmersive.android.repository.HomeRepository
import com.zenimmersive.android.repository.LoginRepository
import com.zenimmersive.android.repository.MyOrdersRepository
import com.zenimmersive.android.repository.PlayerRepository
import com.zenimmersive.android.repository.RegisterRepository
import com.zenimmersive.android.repository.SearchRepository
import com.zenimmersive.android.repository.SettingRepository
import com.zenimmersive.android.repository.SplashRepository
import com.zenimmersive.android.repository.SubscriptionRepository
import com.zenimmersive.android.repository.VerificationRepository
import com.zenimmersive.android.repository.ViewAllAlbumRepository
import com.zenimmersive.android.repository.VipMusicRepository
import com.zenimmersive.android.viewmodel.BlankViewModel
import com.zenimmersive.android.viewmodel.ChangePasswordViewModel
import com.zenimmersive.android.viewmodel.DashboardViewModel
import com.zenimmersive.android.viewmodel.ProfileViewModel
import com.zenimmersive.android.viewmodel.FaqViewModel
import com.zenimmersive.android.viewmodel.FavoritesViewModel
import com.zenimmersive.android.viewmodel.ForgotPasswordViewModel
import com.zenimmersive.android.viewmodel.HomePlaylistViewModel
import com.zenimmersive.android.viewmodel.HomeViewModel
import com.zenimmersive.android.viewmodel.LoginViewModel
import com.zenimmersive.android.viewmodel.MyOrdersViewModel
import com.zenimmersive.android.viewmodel.PlayerViewModel
import com.zenimmersive.android.viewmodel.RegisterViewModel
import com.zenimmersive.android.viewmodel.SearchViewModelModel
import com.zenimmersive.android.viewmodel.SettingViewModel
import com.zenimmersive.android.viewmodel.SplashScreenViewModel
import com.zenimmersive.android.viewmodel.StaticPageViewModel
import com.zenimmersive.android.viewmodel.SubscriptionViewModel
import com.zenimmersive.android.viewmodel.VerificationViewModel
import com.zenimmersive.android.viewmodel.ViewAllAlbumViewModel
import com.zenimmersive.android.viewmodel.VipMusicViewModel

class ViewModelFactory<R>(private val repository: R) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BlankViewModel::class.java)) return BlankViewModel(repository as BlankRepository) as T
        if (modelClass.isAssignableFrom(VerificationViewModel::class.java)) return VerificationViewModel(repository as VerificationRepository) as T
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) return LoginViewModel(repository as LoginRepository) as T
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) return RegisterViewModel(repository as RegisterRepository) as T
        if (modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java)) return ForgotPasswordViewModel(repository as ForgotPasswordRepository) as T
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) return ProfileViewModel(repository as UserProfileRepository) as T
        if (modelClass.isAssignableFrom(SettingViewModel::class.java)) return SettingViewModel(repository as SettingRepository) as T
        if (modelClass.isAssignableFrom(FaqViewModel::class.java)) return FaqViewModel(repository as SettingRepository) as T
        if (modelClass.isAssignableFrom(StaticPageViewModel::class.java)) return StaticPageViewModel(repository as SettingRepository) as T
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) return HomeViewModel(repository as HomeRepository) as T
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) return DashboardViewModel(repository as DashboardRepository) as T
        if (modelClass.isAssignableFrom(HomePlaylistViewModel::class.java)) return HomePlaylistViewModel(repository as HomePlaylistRepository) as T
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) return PlayerViewModel(repository as PlayerRepository) as T
        if (modelClass.isAssignableFrom(SearchViewModelModel::class.java)) return SearchViewModelModel(repository as SearchRepository) as T
        if (modelClass.isAssignableFrom(ChangePasswordViewModel::class.java)) return ChangePasswordViewModel(repository as ChangePasswordRepository) as T
        if (modelClass.isAssignableFrom(FavoritesViewModel::class.java)) return FavoritesViewModel(repository as FavoritesRepository) as T
        if (modelClass.isAssignableFrom(VipMusicViewModel::class.java)) return VipMusicViewModel(repository as VipMusicRepository) as T
        if (modelClass.isAssignableFrom(ViewAllAlbumViewModel::class.java)) return ViewAllAlbumViewModel(repository as ViewAllAlbumRepository) as T
        if (modelClass.isAssignableFrom(MyOrdersViewModel::class.java)) return MyOrdersViewModel(repository as MyOrdersRepository) as T
        if (modelClass.isAssignableFrom(SplashScreenViewModel::class.java)) return SplashScreenViewModel(repository as SplashRepository) as T
        if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) return SubscriptionViewModel(repository as SubscriptionRepository) as T
//        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) return AuthViewModel(repository.context) as T
//        if (modelClass.isAssignableFrom(ChargeViewModel::class.java)) return ChargeViewModel(repository as ChargeRepository) as T
//        if (modelClass.isAssignableFrom(BoxViewModel::class.java)) return BoxViewModel(repository as BoxRepository) as T
//        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) return AccountViewModel(repository as AccountRepository) as T
//        if (modelClass.isAssignableFrom(TermsConditionViewModel::class.java)) return TermsConditionViewModel(repository as TermsConditionRepository) as T
//        if (modelClass.isAssignableFrom(AboutViewModel::class.java)) return AboutViewModel(repository as AboutRepository) as T

        else throw IllegalArgumentException("View Model Class Not Found")
    }
}