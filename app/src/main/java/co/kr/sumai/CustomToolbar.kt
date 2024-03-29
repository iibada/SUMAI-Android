package co.kr.sumai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import co.kr.sumai.caii.CaiiMainActivity
import co.kr.sumai.databinding.ToolbarMainBinding
import co.kr.sumai.func.AvatarSettings
import co.kr.sumai.func.InfoByClass
import co.kr.sumai.func.deletePreferences
import co.kr.sumai.net.service
import co.kr.sumai.voi.VoiMainActivity
import com.bumptech.glide.Glide
import com.facebook.login.LoginManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.user.UserApiClient
import com.nhn.android.naverlogin.OAuthLogin
import kotlinx.android.synthetic.main.toolbar_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomToolbar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : Toolbar(context, attrs), PopupMenu.OnMenuItemClickListener {

    private var accountInformation: AccountInformation? = null
    private var binding = ToolbarMainBinding.inflate(LayoutInflater.from(context),this)

    private val avatar = AvatarSettings()

    private val infoByClass = InfoByClass()

    init{
        attrs?.let { getAttrs(it) }
    }

    private fun getAttrs(attrs : AttributeSet){
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomToolbar)

        setTypeArray(typedArray)
    }

    private fun setTypeArray(typedArray : TypedArray){
        val serviceColor = typedArray.getColor(R.styleable.CustomToolbar_serviceColor,
            ContextCompat.getColor(context, R.color.colorPrimary))
        binding.root.setBackgroundColor(serviceColor)
        binding.loginIcon.setColorFilter(serviceColor)
        binding.loginText.setTextColor(serviceColor)

        val logoSrc = typedArray.getResourceId(R.styleable.CustomToolbar_logoSrc,
            R.drawable.sumai_logo)
        binding.imageSUMAILogo.setImageResource(logoSrc)

        val logoText = typedArray.getString(R.styleable.CustomToolbar_logoText)
        binding.textViewLogo.text = logoText

        binding.buttonApps.setOnClickListener {
            val intent = Intent(context, ServiceListActivity::class.java)
            intent.putExtra("caller", context.javaClass.simpleName)
            intent.putExtra("theme", infoByClass.getTheme(context.javaClass.simpleName))
            context.startActivity(intent)
        }

        with(binding.layoutLogin) {
            visibility = View.GONE
            setOnClickListener{
                val intent = Intent(context, LoginActivity::class.java)
                intent.putExtra("caller", context.javaClass.simpleName)
                context.startActivity(intent)
            }
        }

        with(binding.layoutAccount) {
            visibility = View.GONE
            setOnClickListener {
                val popup = PopupMenu(context, this)
                popup.setOnMenuItemClickListener(this@CustomToolbar)
                popup.menuInflater.inflate(R.menu.account_menu, popup.menu)
                popup.show()
            }
        }

        typedArray.recycle()
    }

    fun init(id: String) {
        if (id.isNotEmpty()) {
            service.loadAccount(id).enqueue(object : Callback<AccountInformation> {
                override fun onResponse(call: Call<AccountInformation>, response: Response<AccountInformation>) {
                    if (response.isSuccessful) {
                        accountInformation = response.body()

                        setAvatar(id)
                    }
                }

                override fun onFailure(call: Call<AccountInformation>, t: Throwable) {
                    Toast.makeText(context, "로그인 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    binding.layoutLogin.visibility = View.VISIBLE
                }
            })
        } else {
            binding.layoutLogin.visibility = View.VISIBLE
        }
    }

    private fun setAvatar(id: String) {
        binding.layoutAccount.visibility = View.VISIBLE

        if (accountInformation!!.image.isNotEmpty()) {  // 프로필 이미지 있으면
            Glide.with(this)
                .load(accountInformation!!.image)
                .circleCrop()
                .into(binding.imageViewAccount)
        } else {  // 프로필 이미지 없으면
            val drawable = ContextCompat.getDrawable(context, R.drawable.circle) as GradientDrawable?
            drawable!!.setColor(Color.parseColor("#" + avatar.toMD5(id).substring(1, 7)))

            Glide.with(this)
                .load(drawable)
                .circleCrop()
                .into(binding.imageViewAccount)

            binding.textViewName.text = avatar.reName(accountInformation!!.name)
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.accountManage -> {
                context.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.sumai.co.kr/accounts?url=${infoByClass.getUrl(context, context.javaClass.simpleName)}")))
                true
            }
            R.id.logout -> {
                val snsType = accountInformation?.type
                val mOAuthLoginModule = OAuthLogin.getInstance()
                mOAuthLoginModule.init(context, context.getString(R.string.naver_client_id),
                    context.getString(R.string.naver_client_secret),
                    context.getString(R.string.app_name))
                when (snsType) {
                    "GOOGLE" -> Firebase.auth.signOut()
                    "KAKAO" -> UserApiClient.instance.logout {}
                    "NAVER" -> mOAuthLoginModule.logout(context)
                    "FACEBOOK" -> LoginManager.getInstance().logOut()
                }
                accountInformation = null
                deletePreferences(context, "loginData", "id")
                deletePreferences(context, "loginData", "name")

                val intent = infoByClass.getIntentByLogo(context, binding.textViewLogo.text.toString())
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val activity = context as Activity
                activity.overridePendingTransition(0, 0)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
                activity.finishAffinity()
                true
            }
            else -> false
        }
    }
}