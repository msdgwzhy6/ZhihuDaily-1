package com.github.jokar.zhihudaily.ui.activity


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import com.github.jokar.zhihudaily.R
import com.github.jokar.zhihudaily.model.entities.MainMenu
import com.github.jokar.zhihudaily.model.rxbus.RxBus
import com.github.jokar.zhihudaily.model.rxbus.event.UpdateThemeEvent
import com.github.jokar.zhihudaily.model.rxbus.event.UpdateToolbarTitleEvent
import com.github.jokar.zhihudaily.presenter.MainPresenter
import com.github.jokar.zhihudaily.ui.adapter.main.MainAdapter
import com.github.jokar.zhihudaily.ui.adapter.viewpager.ViewPagerAdapter
import com.github.jokar.zhihudaily.ui.fragment.MainFragment
import com.github.jokar.zhihudaily.ui.fragment.ThemeFragment
import com.github.jokar.zhihudaily.ui.view.MainView
import com.trello.rxlifecycle2.android.ActivityEvent
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.common_toolbar.*
import javax.inject.Inject
import android.content.Intent
import android.support.v4.view.GravityCompat
import android.view.Menu
import com.github.jokar.zhihudaily.model.rxbus.event.UpdateStoryScrollEvent
import com.jakewharton.rxbinding2.view.RxMenuItem
import java.util.concurrent.TimeUnit


class MainActivity : BaseActivity(), MainView, HasSupportFragmentInjector {
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentInjector
    }

    @Inject
    lateinit var presenter: MainPresenter

    var adapter: MainAdapter? = null
    var menuList: ArrayList<MainMenu>? = null
    var pagerAdapter: ViewPagerAdapter? = null

    var menuChooseIndex: Int = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    fun initView() {
        //initToolBar
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close)
        drawerLayout.setDrawerListener(toggle)
        toggle.syncState()
        //
        recyclerView.layoutManager = LinearLayoutManager(this)
        //设置不可左右滑动
        viewPager.setPagingEnabled(false)
        pagerAdapter = ViewPagerAdapter(supportFragmentManager)

        RxBus.getInstance()
                .toMainThreadObservable(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe { event ->
                    if (event is UpdateToolbarTitleEvent) {
                        toolbar.title = event.title
                    }
                }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)

        RxMenuItem.clicks(menu.getItem(0))
                .throttleFirst(1,TimeUnit.SECONDS)
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe {
                    val intent = Intent(this,SettingActivity::class.java)
                    startActivity(intent)
                }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onWindowInitialized() {
        super.onWindowInitialized()
        presenter.getThemes(bindUntilEvent(ActivityEvent.DESTROY))
    }

    override fun loadThemes(data: ArrayList<MainMenu>) {
        menuList = data

        adapter = MainAdapter(this, bindUntilEvent(ActivityEvent.DESTROY),
                menuList!!)
        recyclerView.adapter = adapter
        adapter?.adapterClickListener = object : MainAdapter.AdapterClickListener {
            override fun itemClickListener(position: Int) {
                if (position != menuChooseIndex) {
                    val menu = menuList?.get(position - 1)
                    menu?.isClick = true
                    menuList?.get(menuChooseIndex - 1)?.isClick = false

                    adapter?.notifyItemChanged(position)
                    adapter?.notifyItemChanged(menuChooseIndex)
                    menuChooseIndex = position
                    if(position == 1){
                        closeDrawaer()
                        toolbar.title = "今日要闻"
                        viewPager.setCurrentItem(0,false)
                        //让fragment滚动到顶部
                        RxBus.getInstance().post(UpdateStoryScrollEvent())
                    }else{
                        closeDrawaer()
                        viewPager.setCurrentItem(1,false)
                        toolbar.title = menu?.name
                        //获取数据
                        RxBus.getInstance().post(UpdateThemeEvent(menu?.id!!))
                    }
                }
            }

            override fun collectionClick() {
                closeDrawaer()
                var intent = Intent(this@MainActivity,CollectionActivity::class.java)
                startActivity(intent)
            }
        }

        pagerAdapter?.addFragment(MainFragment(), "主页")
        pagerAdapter?.addFragment(ThemeFragment(),"主题")
        viewPager.adapter = pagerAdapter

    }

    private fun closeDrawaer() {
        drawerLayout.closeDrawers()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            //返回桌面
            //启动一个意图,回到桌面
            val backHome = Intent(Intent.ACTION_MAIN)
            backHome.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            backHome.addCategory(Intent.CATEGORY_HOME)
            startActivity(backHome)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        menuList = null
    }
}
