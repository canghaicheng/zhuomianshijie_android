package com.example.zhuomianshijie.ui.api

import ApiSetting
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.zhuomianshijie.databinding.FragmentApiBinding
import android.widget.AdapterView
import android.content.Context
import android.widget.TextView
import com.example.zhuomianshijie.R
import com.example.zhuomianshijie.utils.ToastUtils

class ApiFragment : Fragment() {
    private var _binding: FragmentApiBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ApiViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApiBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[ApiViewModel::class.java]
        setupViews()
        observeViewModel()
        return binding.root
    }


    private fun setupViews() {
        binding.apply {
            btnSave.setOnClickListener { saveSettings() }
            btnCopy.setOnClickListener { copyCurrentSetting() }
            btnDelete.setOnClickListener { deleteCurrentSetting() }

            // 修改这部分代码
            spinnerApiSettings.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val settings = viewModel.apiSettings.value ?: return
                        // 检查是否点击了"添加"选项
                        if (position == parent?.adapter?.count?.minus(1)) {
                            clearForm()
                            return
                        }
                        
                        if (position >= 0 && position < settings.size) {
                            val setting = settings[position]
                            fillFormWithSetting(setting)
                            viewModel.setCurrentSetting(setting)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // 可以不做任何处理
                    }
                }
        }
    }


    private fun observeViewModel() {
        viewModel.apiSettings.observe(viewLifecycleOwner) { settings ->
            // 使用自定义适配器
            val adapter = ApiSettingsAdapter(
                requireContext(),
                settings
            )
            binding.spinnerApiSettings.adapter = adapter
        }

        viewModel.currentSetting.observe(viewLifecycleOwner) { setting ->
            setting?.let {
                val position =
                    viewModel.apiSettings.value?.indexOfFirst { s -> s.name == it.name } ?: -1
                if (position >= 0) {
                    binding.spinnerApiSettings.setSelection(position)
                    fillFormWithSetting(it)
                }
            }
        }
    }

    // 自定义适配器，添加"添加自定义API设置"选项
    private inner class ApiSettingsAdapter(
        context: Context,
        private val settings: List<ApiSetting>
    ) : ArrayAdapter<ApiSetting>(context, R.layout.item_spinner_dropdown, settings) {

        override fun getCount(): Int = settings.size + 1

        override fun getItem(position: Int): ApiSetting? {
            return if (position < settings.size) settings[position] else null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // 当前选中项的显示
            if (position == settings.size) {
                return LayoutInflater.from(context)
                    .inflate(R.layout.item_spinner_dropdown, parent, false).apply {
                        findViewById<TextView>(android.R.id.text1).apply {
                            text = "请选择API设置"
                            setTextColor(context.getColor(android.R.color.darker_gray))
                        }
                    }
            }
            
            val view = super.getView(position, convertView, parent)
            (view as? TextView)?.apply {
                text = settings[position].name
                setTextColor(context.getColor(android.R.color.black))
            }
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            // 下拉列表中的显示
            if (position == settings.size) {
                return LayoutInflater.from(context)
                    .inflate(R.layout.item_spinner_add, parent, false)
            }

            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_spinner_dropdown, parent, false)
            (view as? TextView)?.apply {
                text = settings[position].name
                setTextColor(context.getColor(android.R.color.black))
            }
            return view
        }
    }

    private fun clearForm() {
        binding.apply {
            etName.setText("")
            etApiDomain.setText("")
            etApiPath.setText("")
            etApiKey.setText("")
            etModel.setText("")
            etMaxSize.setText("")
            // 可以设置默认值
            spinnerApiMode.setSelection(0)
        }
    }

    private fun fillFormWithSetting(setting: ApiSetting) {
        binding.apply {
            etName.setText(setting.name)
            etApiDomain.setText(setting.apiDomain)
            etApiPath.setText(setting.apiPath)
            etApiKey.setText(setting.apiKey)
            etModel.setText(setting.model)
            etMaxSize.setText(setting.maxSize.toString())
        }
    }

    private fun saveSettings() {
        // 获取所有输入值
        val name = binding.etName.text.toString().trim()
        val apiMode = "openai"
        val apiDomain = binding.etApiDomain.text.toString().trim()
        val apiPath = binding.etApiPath.text.toString().trim()
        val apiKey = binding.etApiKey.text.toString().trim()
        val model = binding.etModel.text.toString().trim()
        val maxSizeStr = binding.etMaxSize.text.toString().trim()
        
        // 校验必填项
        if (name.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入名称")
            return
        }
        
        if (apiDomain.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入API域名")
            return
        }
        
        if (apiPath.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入API路径")
            return
        }
        
        if (apiKey.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入API密钥")
            return
        }
        
        if (model.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入模型名称")
            return
        }
        
        if (maxSizeStr.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入上下文限制")
            return
        }
        
        val maxSize = maxSizeStr.toIntOrNull()
        if (maxSize == null || maxSize <= 0) {
            ToastUtils.showToast(requireContext(), "上下文限制必须是大于0的数字")
            return
        }

        // 创建ApiSetting对象
        val setting = ApiSetting(
            name = name,
            apiMode = apiMode,
            apiDomain = apiDomain,
            apiPath = apiPath,
            apiKey = apiKey,
            model = model,
            maxSize = maxSize
        )

        // 检查是否正在编辑现有设置
        val currentSetting = viewModel.currentSetting.value
        if (currentSetting != null && viewModel.apiSettings.value?.any { it.name == currentSetting.name } == true) {
            // 更新现有设置
            viewModel.updateSetting(setting)
            ToastUtils.showToast(requireContext(), "更新成功")
        } else {
            // 创建新设置
            viewModel.saveSetting(setting)
            ToastUtils.showToast(requireContext(), "保存成功")
        }
    }

    private fun copyCurrentSetting() {
        viewModel.currentSetting.value?.let { setting ->
            // 复制设置并获取新创建的设置
            val newSetting = viewModel.copySetting(setting)
            
            // 显示成功提示
            ToastUtils.showToast(requireContext(), "复制成功")
            
            // 设置当前选中的设置为新复制的设置
            newSetting?.let {
                viewModel.setCurrentSetting(it)
            }
        }
    }

    private fun deleteCurrentSetting() {
        viewModel.currentSetting.value?.let { setting ->
            viewModel.deleteSetting(setting)
            ToastUtils.showToast(requireContext(), "删除成功")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}