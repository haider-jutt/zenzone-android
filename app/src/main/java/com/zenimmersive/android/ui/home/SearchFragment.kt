package com.zenimmersive.android.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.zenimmersive.android.R
import com.zenimmersive.android.adapter.FilterResultSubAlbumAdapter
import com.zenimmersive.android.adapter.HomeDefaultSearchTagsAdapter
import com.zenimmersive.android.adapter.HomeSearchPlaylistAdapter
import com.zenimmersive.android.adapter.SearchFilterTagAdapter
import com.zenimmersive.android.adapter.SearchTagsAdapter
import com.zenimmersive.android.adapter.SubAlbumSongsAdapter
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.apiresponsemodel.FilterResponseModel
import com.zenimmersive.android.apiresponsemodel.Tag
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.FragmentSearchBinding
import com.zenimmersive.android.databinding.SearchFilterSheetViewBinding
import com.zenimmersive.android.helper.Constants.All_SONGS
import com.zenimmersive.android.helper.Constants.SONG_DATA
import com.zenimmersive.android.helper.Constants.SONG_ID
import com.zenimmersive.android.helper.hideKeyboard
import com.zenimmersive.android.repository.SearchRepository
import com.zenimmersive.android.ui.BackPressListener
import com.zenimmersive.android.ui.DashboardActivity
import com.zenimmersive.android.viewmodel.SearchViewModelModel
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog


class SearchFragment :
    BaseFragment<SearchViewModelModel, FragmentSearchBinding, SearchRepository>(),
    BackPressListener, HomeDefaultSearchTagsAdapter.DefaultSearchTagsListener,
    HomeSearchPlaylistAdapter.SearchFilterSongsAdapterListener,
    SubAlbumSongsAdapter.SubAlbumSongAdapterListener {

    lateinit var searchDefaultTagsAdapter: HomeDefaultSearchTagsAdapter
    lateinit var searchResultAdapter: HomeSearchPlaylistAdapter
    private var tagsList: ArrayList<Tag?> = arrayListOf()
    private var songsList: ArrayList<AlbumMusic?> = arrayListOf()
    private var searchTagsList: ArrayList<Tag?> = arrayListOf()
    private var filterList: ArrayList<FilterResponseModel.Result?> = arrayListOf()
    private var lastFilterTagList: ArrayList<Tag?> = arrayListOf()
    private var lastFilterStartDuration: Float? = 5F
    private var lastFilterEndDuration: Float? = 30F
    lateinit var searchTagsAdapter: SearchTagsAdapter
    lateinit var filterResultSubAlbumAdapter: FilterResultSubAlbumAdapter

    override fun getViewModel(): Class<SearchViewModelModel> {
        return SearchViewModelModel::class.java
    }

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSearchBinding {
        return FragmentSearchBinding.inflate(inflater, container, false)
    }

    @OptIn(UnstableApi::class) override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLoader = viewBinding.invLoader.viewLoader
        viewBinding.root.post { hideLoader() }

        if (activity is DashboardActivity) {
            (activity as DashboardActivity).viewBinding.tvPageTitle.text = ""
        }

        viewBinding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            viewBinding.etSearch.text?.length?.let { viewBinding.etSearch.setSelection(it) }
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewBinding.root.hideKeyboard()
                if (!viewBinding.etSearch.text.toString().isNullOrBlank()) {
                    performSearch(viewBinding.etSearch.text.toString())
                }
            }
            return@setOnEditorActionListener true
        }

        viewBinding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank()) {
                    updateVisibilityDefaultTagView(true)
                    viewBinding.llSearchData.visibility = GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        setupRecycleView()
        initObserver()
        initClick()
        viewModel.getTagList()
    }

    private fun updateVisibilityDefaultTagView(showDefaultView: Boolean) {
        if (showDefaultView) {
            viewBinding.tvBrowseAll.visibility = VISIBLE
            viewBinding.rcvDefaultTags.visibility = VISIBLE
            viewBinding.llSearchTagListing.visibility = GONE
            viewBinding.llSearchSongsListing.visibility = GONE
            viewBinding.rcvFilterSubAlbum.visibility = GONE
            viewBinding.llNotFoundData.visibility = GONE
        } else {
            viewBinding.tvBrowseAll.visibility = GONE
            viewBinding.rcvDefaultTags.visibility = GONE
        }
    }

    private fun performSearch(query: String) {
        // Implement your search logic here
        viewModel.searchSongs(query) // Example of calling a method in your ViewModel
    }


    private fun initObserver() {
        viewModel.tagsListData.observe(viewLifecycleOwner) { resTags ->
            when (resTags) {
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
//                    showLoader("Loading...")
                }

                is ViewState.Data -> {
                    hideLoader()
                    tagsList.clear()
                    if (!resTags.data.result.isNullOrEmpty()) {
                        tagsList.addAll(resTags.data.result!!)
                    }
                    updateTagRecyclerView()
                }

                is ViewState.Error -> {
                    hideLoader()
                    showToast(resTags.error)
                }

            }
        }

        viewModel.tagWiseMusicData.observe(viewLifecycleOwner) { resData ->
            when (resData) {
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader("Loading...")
                }

                is ViewState.Data -> {
                    hideLoader()
                    if (!resData.data.result.isNullOrEmpty()) {
                        if (!resData.data.result?.get(0)?.songAlbums.isNullOrEmpty()) {
                            songsList.clear()
                            songsList.addAll(resData.data.result?.get(0)?.songAlbums!!)
                        }
                    }
                    updateSongsResultRecyclerView(songsList)
                    updateNoDataLayout()
                }

                is ViewState.Error -> {
                    hideLoader()
                    showToast(resData.error)
                }
            }
        }

        viewModel.searchSongsData.observe(viewLifecycleOwner) { resData ->
            when (resData) {
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader(getString(R.string.loading))
                }

                is ViewState.Data -> {
                    hideLoader()
                    searchTagsList.clear()
                    songsList.clear()
                    if (!resData.data.result?.albumMusic.isNullOrEmpty()) {
                        songsList.addAll(resData.data.result?.albumMusic ?: arrayListOf())
                    }
                    if (!resData.data.result?.tag.isNullOrEmpty()) {
                        searchTagsList.addAll(resData.data.result?.tag ?: arrayListOf())
                    }
                    setupRecycleView()
                    updateSearchTagsRecyclerView(searchTagsList)
                    updateSongsResultRecyclerView(songsList)
                    updateNoDataLayout()
                }

                is ViewState.Error -> {
                    hideLoader()
                    showToast(resData.error)
                }
            }
        }

        viewModel.filterSongsData.observe(viewLifecycleOwner) { resData ->
            when (resData) {
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader("Loading...")
                }

                is ViewState.Data -> {
                    hideLoader()
                    filterList?.clear()
                    if (!resData.data.result.isNullOrEmpty()) {
                        filterList.addAll(resData.data.result!!)
                    }
                    filterResultUi(filterList)
                }

                is ViewState.Error -> {
                    hideLoader()
                    showToast(resData.error)
                }
            }
        }
    }

    private fun updateNoDataLayout() {
        if(tagsList.isEmpty() && songsList.isEmpty()) {
            viewBinding.llNotFoundData.visibility = VISIBLE
        } else {
            viewBinding.llNotFoundData.visibility = GONE
        }
    }

    private fun updateSongsResultRecyclerView(newSongList: ArrayList<AlbumMusic?>) {
        viewBinding.llSearchData.visibility = VISIBLE
        updateVisibilityDefaultTagView(false)
        if (!newSongList.isNullOrEmpty()) {
            viewBinding.llSearchSongsListing.visibility = VISIBLE
        } else {
            viewBinding.llSearchSongsListing.visibility = GONE
        }
        searchResultAdapter.updateSearchSongList(newSongList)
    }

    private fun updateSearchTagsRecyclerView(newTagList: ArrayList<Tag?>) {
        if (!newTagList.isNullOrEmpty()) {
            viewBinding.llSearchTagListing.visibility = VISIBLE
        } else {
            viewBinding.llSearchTagListing.visibility = GONE
        }
    }

    private fun updateTagRecyclerView() {
        if (!tagsList.isNullOrEmpty()) {
            viewBinding.rcvTag.visibility = VISIBLE
        } else {
            viewBinding.rcvTag.visibility = GONE
        }
        searchDefaultTagsAdapter.notifyDataSetChanged()
    }

    private fun filterResultUi(filterList: ArrayList<FilterResponseModel.Result?>) {
        filterResultSubAlbumAdapter.updateNewFilterList(filterList)
        updateVisibilityDefaultTagView(false)
        if (!filterList.isNullOrEmpty()) {
            viewBinding.llNotFoundData.visibility = GONE
            viewBinding.llSearchData.visibility = GONE
            viewBinding.rcvFilterSubAlbum.visibility = VISIBLE
        } else {
            viewBinding.llNotFoundData.visibility = VISIBLE
            viewBinding.llSearchData.visibility = GONE
            viewBinding.rcvFilterSubAlbum.visibility = GONE
        }
    }

    private fun initClick() {
        viewBinding.icBack.setOnClickListener {
            if (!onBackPressed()) activity?.onBackPressed()
        }

        viewBinding.ivFilter.setOnClickListener {
            viewBinding.ivFilter.isEnabled = false;
            viewBinding.root.postDelayed({
                viewBinding.ivFilter.isEnabled = true;
            },500)
            val filtersBottomSheet = BottomSheetDialog(requireContext())
            val filterBottomSheetBinding =
                SearchFilterSheetViewBinding.inflate(filtersBottomSheet.layoutInflater)
            filtersBottomSheet.setContentView(filterBottomSheetBinding.root)
            filtersBottomSheet.setCancelable(false)

            var filterTagList: ArrayList<Tag?> = arrayListOf()
            if (!lastFilterTagList.isNullOrEmpty()) {
                filterTagList = lastFilterTagList // Create a new list with copied items
            } else {
                filterTagList =
                    ArrayList(tagsList.map { it?.copy() }) // Create a new list with copied items
            }

            filterBottomSheetBinding.rsDuration.setValues(
                lastFilterStartDuration,
                lastFilterEndDuration
            )

            val tagAdapter =
                SearchFilterTagAdapter(filtersBottomSheet.context, filterTagList, object :
                    SearchFilterTagAdapter.TagsListListener {
                    override fun tagItemClick(position: Int, data: Tag?) {

                    }
                })

            val layoutManager = FlexboxLayoutManager(context)
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.FLEX_START
            filterBottomSheetBinding.rcvTag.layoutManager = layoutManager
//            filterBottomSheetBinding.rcvTag.layoutManager = GridLayoutManager(filtersBottomSheet.context, 4)
            filterBottomSheetBinding.rcvTag.adapter = tagAdapter

            filtersBottomSheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            val customThumbDrawable =
                ContextCompat.getDrawable(filtersBottomSheet.context, R.drawable.ic_slide_thumb)
            filterBottomSheetBinding.rsDuration.setCustomThumbDrawable(customThumbDrawable!!)

            filterBottomSheetBinding.btnApply.setOnClickListener {
                val selectedTagsList = filterTagList.filter {
                    it?.selected == true
                }.map {
                    it?.tagId
                }
                val tagsParamValue =
                    selectedTagsList.toString().replace("[", "").replace("]", "").replace(" ", "")
                Log.d("filtersheet", "selected tags id: $tagsParamValue")
                if (selectedTagsList.isNullOrEmpty()) {
                    showToast(getString(R.string.please_select_at_least_1_tag))
                } else {
                    filtersBottomSheet.dismiss()
                    lastFilterTagList = filterTagList
                    lastFilterStartDuration = filterBottomSheetBinding.rsDuration.values[0]
                    lastFilterEndDuration = filterBottomSheetBinding.rsDuration.values[1]
                    viewBinding.viewFilterDot.visibility = VISIBLE
                    viewModel.filterSongs(
                        tagsParamValue,
                        (filterBottomSheetBinding.rsDuration.values[0] * 60).toInt().toString(),
                        (filterBottomSheetBinding.rsDuration.values[1] * 60).toInt().toString()
                    )
                }
            }

            filterBottomSheetBinding.tvClear.setOnClickListener {
                viewBinding.viewFilterDot.visibility = GONE
                lastFilterTagList = arrayListOf()
                lastFilterStartDuration = 5F
                lastFilterEndDuration = 30F
                filtersBottomSheet.dismiss()
                viewBinding.rcvSongsSearchResult.visibility = GONE
                updateVisibilityDefaultTagView(true)
            }

            filterBottomSheetBinding.ivClose.setOnClickListener {
                filtersBottomSheet.dismiss()
            }

            filtersBottomSheet.show()
        }
    }

    private fun setupRecycleView() {
        viewBinding.rcvDefaultTags.layoutManager = GridLayoutManager(requireContext(), 2)
        searchDefaultTagsAdapter = HomeDefaultSearchTagsAdapter(requireContext(), tagsList, this)
        viewBinding.rcvDefaultTags.adapter = searchDefaultTagsAdapter

        viewBinding.rcvSearchResultTags.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        searchTagsAdapter = SearchTagsAdapter(requireContext(), searchTagsList, this)
        viewBinding.rcvSearchResultTags.adapter = searchTagsAdapter

        viewBinding.rcvSongsSearchResult.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        searchResultAdapter = HomeSearchPlaylistAdapter(requireContext(), songsList, this)
        viewBinding.rcvSongsSearchResult.adapter = searchResultAdapter

        viewBinding.rcvFilterSubAlbum.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        filterResultSubAlbumAdapter =
            FilterResultSubAlbumAdapter(requireContext(), filterList, this)
        viewBinding.rcvFilterSubAlbum.adapter = filterResultSubAlbumAdapter
    }

    override fun getRepository(): SearchRepository {
        return SearchRepository(requireContext())
    }

    override fun registerObservers() {

    }

    override fun unregisterObservers() {

    }

    override fun onBackPressed(): Boolean {
        if (!viewBinding.rcvDefaultTags.isVisible) {
            if (viewBinding.llSearchData.isVisible) {
                viewBinding.llSearchData.visibility = GONE
                updateVisibilityDefaultTagView(true)
            } else if (viewBinding.rcvFilterSubAlbum.isVisible) {
                viewBinding.rcvFilterSubAlbum.visibility = GONE
                updateVisibilityDefaultTagView(true)
            }
            return true
        }
        return false
    }

    override fun defaultTagItemClick(position: Int, data: Tag) {
        data.tagId?.let {
            viewModel.getTagsSongs(it)
        }
    }

    @OptIn(UnstableApi::class)
    override fun searchFilterSongsItemClick(position: Int, songsData: AlbumMusic) {
        if(activity is DashboardActivity){
            val bundle = Bundle()
            bundle.putInt(SONG_ID, songsData.songId!!)
            bundle.putSerializable(SONG_DATA, songsData)
            bundle.putSerializable(All_SONGS, songsList)
//            if (songsData.isPurchased != 1){
            /*if (songsData != null && KeyStorage.getInstance(requireActivity()).shouldShowSubscriptionSheet(requireActivity(), songsData)){
                (activity as DashboardActivity).displayPurchasePage(bundle)
            }else{*/
                (activity as DashboardActivity).displayPlayerPage(bundle)
//            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onSubAlbumSongClick(
        subAlbumSongsList: ArrayList<AlbumMusic?>?,
        songData: AlbumMusic
    ) {
        if(activity is DashboardActivity){
            val bundle = Bundle()
            bundle.putInt(SONG_ID, songData.songId!!)
            bundle.putSerializable(SONG_DATA, songData)
            bundle.putSerializable(All_SONGS, subAlbumSongsList)
//            if (songData.isPurchased != 1){
            // The following line is commented out to always open the player fragment regardless of purchase/subscription status
           /* if (songData != null && KeyStorage.getInstance(requireActivity()).shouldShowSubscriptionSheet(requireActivity(), songData)){
                (activity as DashboardActivity).displayPurchasePage(bundle)
            }else{*/
                (activity as DashboardActivity).displayPlayerPage(bundle)
//            }
        }
    }

}