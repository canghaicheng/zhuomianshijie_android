package com.example.zhuomianshijie.ui.tts

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
import com.example.zhuomianshijie.databinding.FragmentTtsBinding
import TtsSetting
import com.example.zhuomianshijie.utils.ToastUtils

class TtsFragment : Fragment() {
    private var _binding: FragmentTtsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TtsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTtsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[TtsViewModel::class.java]
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
            spinnerTtsSettings.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val settings = viewModel.ttsSettings.value ?: return
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
        viewModel.ttsSettings.observe(viewLifecycleOwner) { settings ->
            // 使用自定义适配器
            val adapter = TtsSettingsAdapter(
                requireContext(),
                settings
            )
            binding.spinnerTtsSettings.adapter = adapter
        }

        viewModel.currentSetting.observe(viewLifecycleOwner) { setting ->
            setting?.let {
                val position =
                    viewModel.ttsSettings.value?.indexOfFirst { s -> s.name == it.name } ?: -1
                if (position >= 0) {
                    binding.spinnerTtsSettings.setSelection(position)
                    fillFormWithSetting(it)
                }
            }
        }
    }

    // 自定义适配器，添加"添加自定义TTS设置"选项
    private inner class TtsSettingsAdapter(
        context: Context,
        private val settings: List<TtsSetting>
    ) : ArrayAdapter<TtsSetting>(context, R.layout.item_spinner_dropdown, settings) {

        override fun getCount(): Int = settings.size + 1

        override fun getItem(position: Int): TtsSetting? {
            return if (position < settings.size) settings[position] else null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // 当前选中项的显示
            if (position == settings.size) {
                return LayoutInflater.from(context)
                    .inflate(R.layout.item_spinner_dropdown, parent, false).apply {
                        findViewById<TextView>(android.R.id.text1).apply {
                            text = "请选择TTS设置"
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
                    .inflate(R.layout.item_spinner_tts_add, parent, false)
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
            etTtsDomain.setText("")
            etTtsPath.setText("")
            etTtsKey.setText("")
            etTtsModel.setText("")
            etTtsVoiceId.setText("")
        }
    }

    private fun fillFormWithSetting(setting: TtsSetting) {
        binding.apply {
            etName.setText(setting.name)
            etTtsDomain.setText(setting.ttsDomain)
            etTtsPath.setText(setting.ttsPath)
            etTtsKey.setText(setting.ttsKey)
            etTtsModel.setText(setting.ttsModel)
            etTtsVoiceId.setText(setting.ttsVoiceId)
        }
    }

    private fun saveSettings() {
        // 获取所有输入值
        val name = binding.etName.text.toString().trim()
        val ttsDomain = binding.etTtsDomain.text.toString().trim()
        val ttsPath = binding.etTtsPath.text.toString().trim()
        val ttsKey = binding.etTtsKey.text.toString().trim()
        val ttsModel = binding.etTtsModel.text.toString().trim()
        val ttsVoiceId = binding.etTtsVoiceId.text.toString().trim()
        
        // 校验必填项
        if (name.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入名称")
            return
        }
        
        if (ttsDomain.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入TTS API域名")
            return
        }
        
        if (ttsPath.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入TTS API路径")
            return
        }
        
        if (ttsKey.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入TTS API密钥")
            return
        }
        
        if (ttsModel.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入TTS模型")
            return
        }
        
        if (ttsVoiceId.isEmpty()) {
            ToastUtils.showToast(requireContext(), "请输入音色编号")
            return
        }

        // 创建TtsSetting对象
        val setting = TtsSetting(
            name = name,
            ttsDomain = ttsDomain,
            ttsPath = ttsPath,
            ttsKey = ttsKey,
            ttsModel = ttsModel,
            ttsVoiceId = ttsVoiceId
        )

        // 检查是否正在编辑现有设置
        val currentSetting = viewModel.currentSetting.value
        if (currentSetting != null && viewModel.ttsSettings.value?.any { it.name == currentSetting.name } == true) {
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