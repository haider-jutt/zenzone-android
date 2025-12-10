package com.zenimmersive.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.zenimmersive.android.adapter.FaqAdapter
import com.zenimmersive.android.apiresponsemodel.FaqResModel
import com.zenimmersive.android.base.BaseFragment
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.FragmentFAQBinding
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_ID
import com.zenimmersive.android.helper.KeyStorage.Companion.KEY_USER_TOKEN
import com.zenimmersive.android.repository.SettingRepository
import com.zenimmersive.android.ui.BackPressListener
import com.zenimmersive.android.viewmodel.FaqViewModel

class FAQFragment : BaseFragment<FaqViewModel, FragmentFAQBinding, SettingRepository>(), BackPressListener {

    lateinit var faqAdapter: FaqAdapter
    var userID: Int = 0
    lateinit var userToken: String
    var faqList: ArrayList<FaqResModel.Result?> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userID = KeyStorage.getInstance(requireContext()).getInt(KEY_USER_ID)
        userToken = KeyStorage.getInstance(requireContext()).getString(KEY_USER_TOKEN)
    }

    override fun getViewModel(): Class<FaqViewModel> {
        return FaqViewModel::class.java
    }

    override fun getActivityBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentFAQBinding {
        return FragmentFAQBinding.inflate(inflater,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObserver()
        val resModel = FaqResModel(null, null, null)
        viewModel.faqGetList(userID, userToken,resModel)
        viewBinding.ivBack.setOnClickListener {
            activity?.onBackPressed()
        }

        viewBinding.rcvFaq.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL,false)
        faqAdapter = FaqAdapter(requireContext(), faqList)
        viewBinding.rcvFaq.adapter = faqAdapter
    }

    override fun getRepository(): SettingRepository {
        return SettingRepository(requireContext())
    }

    override fun registerObservers() {
    }

    override fun unregisterObservers() {
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    private fun initObserver(){
        viewModel.faqData.observe(viewLifecycleOwner){ resModel ->
            when(resModel){
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader("Loading...")
                }
                is ViewState.Error -> {
                    hideLoader()
                    showToast(resModel.error)
                    viewBinding.emptyMessage.visibility = View.VISIBLE
                }
                is ViewState.Data -> {
                    hideLoader()
                    faqList.clear()
                    resModel.data.result?.let {
                        faqList.addAll(resModel.data.result!!)
                    }
                    faqAdapter.notifyDataSetChanged()
                    if (faqList.isEmpty()){
                        viewBinding.emptyMessage.visibility = View.VISIBLE
                    }else{
                        viewBinding.emptyMessage.visibility = View.GONE
                    }
                }
            }

        }
    }
}