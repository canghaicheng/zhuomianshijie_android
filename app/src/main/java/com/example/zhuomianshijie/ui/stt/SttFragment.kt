package com.example.zhuomianshijie.ui.stt

import SttSetting
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.zhuomianshijie.R
import com.example.zhuomianshijie.databinding.FragmentSttBinding
import com.example.zhuomianshijie.utils.ToastUtils

class SttFragment : Fragment() {
    private var _binding: FragmentSttBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SttViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSttBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[SttViewModel::class.java]
        setupViews()
        observeViewModel()
        return binding.root
    }

    private fun setupViews() {
        binding.apply {
            btnSave.setOnClickListener { saveSettings() }
            btnCopy.setOnClickListener { copyCurrentSetting() }
            btnDelete.setOnClickListener { deleteCurrentSetting() }

            // 设置下拉菜单选择监听器
            spinnerSttSettings.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val settings = viewModel.sttSettings.value ?: return
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
        viewModel.sttSettings.observe(viewLifecycleOwner) { settings ->
            // 使用自定义适配器
            val adapter = SttSettingsAdapter(
                requireContext(),
                settings
            )
            binding.spinnerSttSettings.adapter = adapter
        }

        viewModel.currentSetting.observe(viewLifecycleOwner) { setting ->
            setting?.let {
                val position =
                    viewModel.sttSettings.value?.indexOfFirst { s -> s.name == it.name } ?: -1
                if (position >= 0) {
                    binding.spinnerSttSettings.setSelection(position)
                    fillFormWithSetting(it)
                }
            }
        }
    }

    // 自定义适配器，添加"添加自定义STT设置"选项
    private inner class SttSettingsAdapter(
        context: Context,
        private val settings: List<SttSetting>
    ) : ArrayAdapter<SttSetting>(context, R.layout.item_spinner_dropdown, settings) {

        override fun getCount(): Int = settings.size + 1

        override fun getItem(position: Int): SttSetting? {
            return if (position < settings.size) settings[position] else null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // 当前选中项的显示
            if (position == settings.size) {
                return LayoutInflater.from(context)
                    .inflate(R.layout.item_spinner_dropdown, parent, false).apply {
                        findViewById<TextView>(android.R.id.text1).apply {
                            text = "请选择STT设置"
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
                    .inflate(R.layout.item_spinner_stt_add, parent, false)
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
            etSttDomain.setText("")
            etSttPath.setText("")
            etSttKey.setText("")
            etSttModel.setText("")
        }
    }

    private fun fillFormWithSetting(setting: SttSetting) {
        binding.apply {
            etName.setText(setting.name)
            etSttDomain.setText(setting.sttDomain)
            etSttPath.setText(setting.sttPath)
            etSttKey.setText(setting.sttKey)
            etSttModel.setText(setting.sttModel)
        }
    }

    private fun saveSettings() {
        // 获取所有输入值
        val name = binding.etName.text.toString().trim()
        val sttDomain = binding.etSttDomain.text.toString().trim()
        val sttPath = binding.etSttPath.text.toString().trim()
        val sttKey = binding.etSttKey.text.toString().trim()
        val sttModel = binding.etSttModel.text.toString().trim()
        
        // 校验必填项
        if (name.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入名称")
            return
        }
        
        if (sttDomain.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入STT API域名")
            return
        }
        
        if (sttPath.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入STT API路径")
            return
        }
        
        if (sttKey.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入STT API密钥")
            return
        }
        
        if (sttModel.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入STT模型")
            return
        }

        // 创建SttSetting对象
        val setting = SttSetting(
            name = name,
            sttDomain = sttDomain,
            sttPath = sttPath,
            sttKey = sttKey,
            sttModel = sttModel
        )

        // 检查是否正在编辑现有设置
        val currentSetting = viewModel.currentSetting.value
        if (currentSetting != null && viewModel.sttSettings.value?.any { it.name == currentSetting.name } == true) {
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