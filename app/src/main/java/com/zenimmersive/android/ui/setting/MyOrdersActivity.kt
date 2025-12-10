package com.zenimmersive.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.zenimmersive.android.adapter.MyOrdersAdapter
import com.zenimmersive.android.apiresponsemodel.OrderListResModel
import com.zenimmersive.android.base.BaseActivity
import com.zenimmersive.android.base.ViewState
import com.zenimmersive.android.databinding.ActivityMyOrdersBinding
import com.zenimmersive.android.helper.ContextWrapper
import com.zenimmersive.android.helper.KeyStorage
import com.zenimmersive.android.repository.MyOrdersRepository
import com.zenimmersive.android.viewmodel.MyOrdersViewModel

class MyOrdersActivity : BaseActivity<MyOrdersViewModel, ActivityMyOrdersBinding, MyOrdersRepository>() {
    private val TAG = MyOrdersActivity::class.java.simpleName
    private lateinit var orderAdapter : MyOrdersAdapter
    private var ordersList: ArrayList<OrderListResModel.Result?> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding.ivBack.setOnClickListener {
            onBackPressed()
        }

        viewBinding.rcvOrders.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        orderAdapter = MyOrdersAdapter(this, ordersList)
        viewBinding.rcvOrders.adapter = orderAdapter

        viewModel.getMyOrders()

        initObserver()
    }

    private fun initObserver() {
        viewModel.myOrdersListRes.observe(this) { resData ->
            when (resData) {
                is ViewState.DefaultState -> {}
                is ViewState.Loading -> {
                    showLoader("Loading...")
                }
                is ViewState.Data -> {
                    hideLoader()
                    if (resData.data.status == 1){
                        resData?.data?.result?.let {
                            ordersList.clear()
                            ordersList.addAll(it)
                        }
                        ordersList?.forEach { item ->
                            if(item?.musicPackId?.isNullOrEmpty() == false){
                                KeyStorage.getInstance(ContextWrapper.getContext()).storePurchasePackId(item?.musicPackId?.toInt())
                            }
                        }

                    }
                    updateUi()
                }
                is ViewState.Error -> {
                    hideLoader()
                    showToast(resData.error)
                    updateUi()
                }
            }
        }
    }

    fun updateUi(){
        if (!ordersList.isNullOrEmpty()){
            viewBinding.rcvOrders.visibility = View.VISIBLE
            viewBinding.llNotFoundData.visibility = View.GONE
            orderAdapter.notifyDataSetChanged()
        }else{
            viewBinding.rcvOrders.visibility = View.GONE
            viewBinding.llNotFoundData.visibility = View.VISIBLE
        }
    }

    override fun getViewModel(): Class<MyOrdersViewModel> {
        return MyOrdersViewModel::class.java
    }

    override fun getActivityBinding(inflater: LayoutInflater): ActivityMyOrdersBinding {
        return ActivityMyOrdersBinding.inflate(inflater)
    }

    override fun getRepository(): MyOrdersRepository {
        return MyOrdersRepository(this)
    }

    override fun bindViewModel() {

    }

    override fun removeViewModelCallbacks() {

    }
}