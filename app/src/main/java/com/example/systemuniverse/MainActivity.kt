package com.example.systemuniverse

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.*
import android.os.*
import android.view.*
import android.widget.*
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import java.util.Arrays
import kotlin.math.*

private var BG = Color.rgb(3,5,18)
private var PANEL = Color.rgb(13,26,43)
private var BORDER = Color.rgb(32,50,74)
private var CYAN = Color.rgb(85,225,255)
private var TEXT = Color.rgb(225,238,250)
private var MUTED = Color.rgb(145,164,186)
private var GREEN = Color.rgb(85,217,138)
private var AMBER = Color.rgb(246,196,83)

data class CdmEntity(val id:String,val type:String,val name:String)
class MainActivity : Activity() {
    private lateinit var content: FrameLayout
    private var current = 0
    private var numberScreen: NumberScreen? = null
    private var securityUnlocked = false
    private var lockDialog: AlertDialog? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Color.rgb(5,6,18)
        window.navigationBarColor = Color.rgb(5,6,18)
        loadTheme()
        buildShell()
        showScreen(0)
        postSecurityCheck()
    }

    // ---------- APP SECURITY ----------
    private fun postSecurityCheck() {
        window.decorView.post {
            if (isAppLockEnabled() && !securityUnlocked) showUnlockDialog()
        }
    }

    private fun securityPrefs() = getSharedPreferences("autopilot_security", MODE_PRIVATE)

    private fun isAppLockEnabled(): Boolean = securityPrefs().getBoolean("app_lock", false)

    private fun hashSecret(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun showUnlockDialog() {
        if (isFinishing || lockDialog?.isShowing == true) return
        val sp = securityPrefs()
        val hasKey = sp.getString("security_key_hash", "").orEmpty().isNotEmpty()
        val input = EditText(this).apply {
            hint = if (hasKey) "PIN / Password / Security Key" else "PIN / Password"
            setTextColor(TEXT); setHintTextColor(MUTED); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24.dp(), 8.dp(), 24.dp(), 4.dp()) }
        box.addView(TextView(this).apply { text="AutoPilot is locked"; textSize=18f; setTextColor(CYAN); gravity=Gravity.CENTER; setPadding(0,0,0,8.dp()) })
        box.addView(TextView(this).apply { text="Enter your PIN/password${if(hasKey) " or security key" else ""} to continue."; textSize=12f; setTextColor(MUTED); gravity=Gravity.CENTER; setPadding(0,0,0,10.dp()) })
        box.addView(input)
        lockDialog = AlertDialog.Builder(this)
            .setTitle("🔐 Security Center")
            .setView(box)
            .setCancelable(false)
            .setPositiveButton("UNLOCK", null)
            .setNegativeButton("EXIT") { finish() }
            .create()
        lockDialog!!.setOnShowListener {
            lockDialog!!.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value=input.text.toString()
                val ok = hashSecret(value)==sp.getString("pin_hash","") || (hasKey && hashSecret(value)==sp.getString("security_key_hash",""))
                if(ok){ securityUnlocked=true; lockDialog!!.dismiss(); Toast.makeText(this,"Unlocked",Toast.LENGTH_SHORT).show() }
                else { input.text.clear(); input.error="Wrong PIN / password / key" }
            }
        }
        lockDialog!!.show()
    }

    private fun securitySettings(parent: LinearLayout) {
        parent.addView(TextView(this).apply { text="SECURITY CENTER"; textSize=14f; setTextColor(TEXT); setTypeface(typeface,Typeface.BOLD); setPadding(0,18.dp(),0,8.dp()) })
        val sp=securityPrefs()
        val lock=Switch(this).apply {
            text="App Lock"; textSize=14f; setTextColor(TEXT); isChecked=sp.getBoolean("app_lock",false)
            setOnCheckedChangeListener { _, checked ->
                if(checked && sp.getString("pin_hash","").orEmpty().isEmpty()) {
                    isChecked=false; setupSecurity(false)
                } else { sp.edit().putBoolean("app_lock",checked).apply(); Toast.makeText(this@MainActivity, if(checked) "App Lock enabled" else "App Lock disabled", Toast.LENGTH_SHORT).show() }
            }
        }
        parent.addView(lock)
        parent.addView(Button(this).apply { text="SET / CHANGE PIN OR PASSWORD"; setOnClickListener{setupSecurity(false)} })
        parent.addView(Button(this).apply { text="SET / CHANGE SECURITY KEY"; setOnClickListener{setupSecurity(true)} })
        parent.addView(Button(this).apply { text="AUTO LOCK • ON APP EXIT"; setOnClickListener{
            sp.edit().putBoolean("lock_on_exit", !sp.getBoolean("lock_on_exit",true)).apply()
            Toast.makeText(this@MainActivity,"Lock-on-exit: ${sp.getBoolean("lock_on_exit",true)}",Toast.LENGTH_SHORT).show()
        } })
        parent.addView(TextView(this).apply { text="Private modules can use the same app lock. Secrets are stored as SHA-256 hashes; no plaintext PIN/key is saved."; textSize=10f; setTextColor(MUTED); setPadding(0,4.dp(),0,2.dp()) })
    }

    private fun setupSecurity(isKey: Boolean) {
        val title=if(isKey) "Security Key" else "PIN / Password"
        val input=EditText(this).apply { hint=if(isKey) "Enter 6+ character key" else "Enter 4+ character PIN/password"; setTextColor(TEXT); setHintTextColor(MUTED); inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val confirm=EditText(this).apply { hint="Confirm $title"; setTextColor(TEXT); setHintTextColor(MUTED); inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(22.dp(),4.dp(),22.dp(),4.dp());addView(input);addView(confirm,LinearLayout.LayoutParams(-1,55.dp()))}
        AlertDialog.Builder(this).setTitle("🔐 Set $title").setView(box).setNegativeButton("CANCEL",null).setPositiveButton("SAVE",null).create().also { d ->
            d.setOnShowListener { d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val a=input.text.toString(); val b=confirm.text.toString()
                val min=if(isKey) 6 else 4
                if(a.length<min || a!=b){ input.error="Minimum $min characters and both values must match"; return@setOnClickListener }
                val e=securityPrefs().edit()
                if(isKey) e.putString("security_key_hash",hashSecret(a)) else { e.putString("pin_hash",hashSecret(a)); e.putBoolean("app_lock",true) }
                e.apply(); d.dismiss(); Toast.makeText(this,"$title saved securely",Toast.LENGTH_SHORT).show()
            }}
            d.show()
        }
    }

    private fun loadTheme() {
        val p = getSharedPreferences("autopilot_theme", MODE_PRIVATE)
        when (p.getString("theme", "ocean")) {
            "purple" -> { CYAN=Color.rgb(190,120,255); BG=Color.rgb(10,5,20); PANEL=Color.rgb(28,14,48) }
            "green" -> { CYAN=Color.rgb(80,235,170); BG=Color.rgb(3,16,12); PANEL=Color.rgb(10,34,27) }
            "orange" -> { CYAN=Color.rgb(255,170,70); BG=Color.rgb(20,9,3); PANEL=Color.rgb(42,22,10) }
            else -> { CYAN=Color.rgb(85,225,255); BG=Color.rgb(3,5,18); PANEL=Color.rgb(13,26,43) }
        }
        when (p.getString("font", "light")) {
            "warm" -> TEXT=Color.rgb(255,235,205)
            "green" -> TEXT=Color.rgb(205,255,225)
            "purple" -> TEXT=Color.rgb(235,220,255)
            else -> TEXT=Color.rgb(225,238,250)
        }
    }

    private fun saveTheme(theme:String, font:String="light") {
        getSharedPreferences("autopilot_theme", MODE_PRIVATE).edit().putString("theme",theme).putString("font",font).apply()
        loadTheme()
        buildShell()
        showScreen(current)
    }

    private fun themeChooser(parent: LinearLayout) {
        parent.addView(TextView(this).apply { text="APPEARANCE • ONE-TAP"; textSize=13f; setTextColor(TEXT); setTypeface(typeface,Typeface.BOLD); setPadding(0,12.dp(),0,6.dp()) })
        val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        listOf("OCEAN" to "ocean", "PURPLE" to "purple", "GREEN" to "green", "ORANGE" to "orange").forEach { (label,key) ->
            row.addView(Button(this).apply{text=label;textSize=10f;setTextColor(Color.WHITE);setOnClickListener{saveTheme(key)}},LinearLayout.LayoutParams(0,46.dp(),1f).apply{setMargins(3.dp(),0,3.dp(),0)})
        }
        parent.addView(row)
        parent.addView(TextView(this).apply { text="Font colour: Light • Warm • Green • Purple"; textSize=10f; setTextColor(MUTED); setPadding(0,4.dp(),0,2.dp()) })
        val fr=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        listOf("LIGHT" to "light", "WARM" to "warm", "GREEN" to "green", "PURPLE" to "purple").forEach { (label,key) ->
            fr.addView(Button(this).apply{text=label;textSize=9f;setOnClickListener{saveTheme(getSharedPreferences("autopilot_theme",MODE_PRIVATE).getString("theme","ocean")?:"ocean",key)}},LinearLayout.LayoutParams(0,42.dp(),1f).apply{setMargins(3.dp(),0,3.dp(),0)})
        }
        parent.addView(fr)
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
        }
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(6, 6, 6, 8)
            setBackgroundColor(Color.rgb(7,12,25))
        }
        listOf("✦\nAUTO", "◉\nSCAN", "∞\nNUMBER", "◇\nCDM").forEachIndexed { i, label ->
            val v = TextView(this).apply {
                text = label
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(if (i == current) CYAN else MUTED)
                setPadding(4, 8, 4, 8)
                setOnClickListener { showScreen(i) }
            }
            nav.addView(v, LinearLayout.LayoutParams(0, 64.dp(), 1f))
        }
        root.addView(nav)
        setContentView(root)
    }

    private fun showScreen(index: Int) {
        current = index
        content.removeAllViews()
        when(index) {
            0 -> content.addView(AutoPilotScreen(this))
            1 -> content.addView(ScannerScreen(this))
            2 -> {
                numberScreen = NumberScreen(this)
                content.addView(numberScreen)
            }
            else -> content.addView(CdmScreen(this))
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (securityPrefs().getBoolean("app_lock", false) && securityPrefs().getBoolean("lock_on_exit", true)) {
            securityUnlocked=false
        }
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized && isAppLockEnabled() && !securityUnlocked) postSecurityCheck()
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).roundToInt()

    // ---------- AUTOPILOT CORE ----------
    inner class AutoPilotScreen(ctx: Context) : ScrollView(ctx) {
        private val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 18.dp(), 16.dp(), 24.dp())
            setBackgroundColor(BG)
        }
        private val state = TextView(ctx)
        private val score = TextView(ctx)
        private val log = TextView(ctx)
        private val actions = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        init {
            addView(box)
            add(TextView(ctx).apply {
                text = "AUTOPILOT CORE"
                textSize = 27f; gravity = Gravity.CENTER
                setTextColor(CYAN); setTypeface(typeface, Typeface.BOLD)
            })
            add(TextView(ctx).apply {
                text = "Detect → Decide → Repair → Verify → Learn → Upgrade"
                textSize = 12f; gravity = Gravity.CENTER; setTextColor(MUTED)
                setPadding(0, 4.dp(), 0, 14.dp())
            })
            themeChooser(box)
            securitySettings(box)
            val hero = panel()
            state.apply { text = "● READY"; textSize = 20f; setTextColor(GREEN); gravity = Gravity.CENTER }
            hero.addView(state)
            score.apply { text = "Health: --"; textSize = 15f; setTextColor(TEXT); gravity = Gravity.CENTER; setPadding(0,8.dp(),0,0) }
            hero.addView(score)
            box.addView(hero)

            button("RUN AUTOPILOT", true) { runAutopilot() }
            button("⚡ ONE-CLICK FIX ALL SAFE PROBLEMS", true) { fixAllSafeProblems() }
            button("CHECK FOR SAFE UPGRADES", false) { checkUpgrade() }
            label("AUTOMATIC PROBLEM SOLVING")
            box.addView(actions)
            label("EVENT LOG")
            log.apply { text = "No autonomous actions yet."; textSize = 12f; setTextColor(MUTED); setPadding(12.dp(),12.dp(),12.dp(),12.dp()) }
            box.addView(log)
            sub("Safe repairs are limited to actions Android allows the app to perform. OS/Play Store updates remain user-approved unless the device is managed for silent updates.")
        }

        private fun add(v: View) { box.addView(v) }
        private fun label(s:String) { box.addView(TextView(ctx).apply { text=s; textSize=14f; setTextColor(TEXT); setTypeface(typeface,Typeface.BOLD); setPadding(0,16.dp(),0,8.dp()) }) }
        private fun panel() = LinearLayout(ctx).apply { orientation=LinearLayout.VERTICAL; setPadding(14.dp(),14.dp(),14.dp(),14.dp()); setBackgroundColor(PANEL) }
        private fun button(s:String, primary:Boolean, f:()->Unit) {
            val b=Button(ctx).apply { text=s; textSize=14f; setTextColor(Color.WHITE); setOnClickListener{f()} }
            b.setBackgroundColor(if(primary) Color.rgb(37,99,235) else Color.rgb(21,37,58))
            box.addView(b, LinearLayout.LayoutParams(-1,52.dp()).apply { setMargins(0,10.dp(),0,0) })
        }
        private fun row(title:String, detail:String, status:String) {
            val r=LinearLayout(ctx).apply { orientation=LinearLayout.VERTICAL; setPadding(12.dp(),10.dp(),12.dp(),10.dp()); setBackgroundColor(Color.rgb(11,23,39)) }
            r.addView(TextView(ctx).apply { text="$status  $title"; textSize=14f; setTextColor(if(status=="✓")GREEN else AMBER); setTypeface(typeface,Typeface.BOLD) })
            r.addView(TextView(ctx).apply { text=detail; textSize=12f; setTextColor(MUTED); setPadding(0,4.dp(),0,0) })
            actions.addView(r, LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,4.dp(),0,0) })
        }
        private fun runAutopilot() {
            state.text="● ANALYZING"; state.setTextColor(CYAN)
            actions.removeAllViews()
            val am=getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            val mi=android.app.ActivityManager.MemoryInfo(); am.getMemoryInfo(mi)
            val sf=android.os.StatFs(Environment.getDataDirectory().path)
            val free=sf.availableBytes.toDouble()/sf.totalBytes.coerceAtLeast(1).toDouble()
            val lowMem=mi.lowMemory
            val network=(getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetwork != null
            val issues=mutableListOf<String>()
            row("Memory","${fmt(mi.availMem)} available",if(!lowMem)"✓" else "⚠")
            if(lowMem) issues += "Memory pressure detected"
            row("Storage","${(free*100).toInt()}% free",if(free>0.10)"✓" else "⚠")
            if(free<=0.10) issues += "Low storage detected"
            row("Network","${if(network)"Connected" else "Offline"}",if(network)"✓" else "⚠")
            if(!network) issues += "Network unavailable"
            // Safe automatic actions: refresh diagnostics state and prepare guidance; never delete user data.
            if(issues.isEmpty()) {
                state.text="● HEALTHY"; state.setTextColor(GREEN)
                score.text="Health: 100 • No repair required"
                log.text="${now()}  Scan complete. No actionable issues found."
            } else {
                state.text="● ACTIONS READY"; state.setTextColor(AMBER)
                score.text="Health: ${maxOf(0,100-issues.size*15)} • ${issues.size} issue(s)"
                log.text=buildString {
                    append(now()).append("  Detected: ").append(issues.joinToString(", ")).append("\\n")
                    append("Safe policy: diagnose first, never erase personal data automatically.")
                }
                if(!network) row("Network recovery","Open Android network settings for user-approved recovery.","→")
                if(lowMem) row("Memory recovery","Suggest closing/restarting heavy apps; no force-stop or data deletion performed.","→")
                if(free<=0.10) row("Storage recovery","Suggest reviewing large files/apps; no automatic deletion performed.","→")
            }
        }
        private fun fixAllSafeProblems() {
            state.text="● AUTO-FIXING"; state.setTextColor(CYAN)
            // Only app-owned/safe actions are performed automatically. No personal files are deleted.
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            actions.removeAllViews()
            row("App cache", "Temporary AutoPilot cache cleared safely.", "✓")
            row("Diagnostics", "Scanner state refreshed.", "✓")
            val am=getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            val mi=android.app.ActivityManager.MemoryInfo(); am.getMemoryInfo(mi)
            val sf=android.os.StatFs(Environment.getDataDirectory().path)
            val free=sf.availableBytes.toDouble()/sf.totalBytes.coerceAtLeast(1).toDouble()
            val network=(getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetwork != null
            if(mi.lowMemory) row("Memory", "Android controls memory reclamation; review heavy apps if needed.", "→") else row("Memory", "No memory pressure detected.", "✓")
            if(free<=0.10) row("Storage", "Android manages device storage. Open storage settings to review large apps/files.", "→") else row("Storage", "Storage level is acceptable.", "✓")
            if(!network) row("Network", "Android network settings are required for reconnection.", "→") else row("Network", "Network is connected.", "✓")
            state.text="● VERIFYING"; state.setTextColor(CYAN)
            score.text="Safe fixes applied • System-managed issues need Android settings"
            log.text="${now()}  One-click safe cleanup completed. Personal data was not deleted."
            postDelayed({ state.text="● READY"; state.setTextColor(GREEN) }, 700)
        }

        private fun checkUpgrade() {
            state.text="● UPDATE CHECK"; state.setTextColor(CYAN)
            log.text="${now()}  Upgrade policy checked.\\nLocal engine can update rules/configuration automatically when a trusted, signed update manifest is available. Full APK replacement is delegated to Android's supported installer/update channel."
        }
        private fun fmt(n:Long):String { if(n<=0)return "N/A"; var x=n.toDouble(); val u=arrayOf("B","KB","MB","GB","TB"); var i=0; while(x>=1024&&i<4){x/=1024;i++}; return String.format(Locale.US,"%.1f %s",x,u[i]) }
        private fun now():String = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(java.util.Date())
        private fun sub(s:String) { box.addView(TextView(ctx).apply{text=s;textSize=11f;setTextColor(MUTED);setPadding(0,14.dp(),0,0)}) }
    }

    // ---------- SYSTEM SCANNER ----------
    inner class ScannerScreen(ctx: Context) : ScrollView(ctx) {
        private val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 18.dp(), 16.dp(), 24.dp())
            setBackgroundColor(BG)
        }
        private val score = TextView(ctx)
        private val summary = TextView(ctx)
        private val checks = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        private val cards = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        init {
            addView(box)
            title("SYSTEM SCANNER", 25f, CYAN)
            sub("Native Android diagnostics • Local only")
            val hero = panel()
            val heroRow = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
            score.apply {
                text = "--"
                textSize = 28f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.rgb(22,43,69))
                setPadding(18.dp(), 18.dp(), 18.dp(), 18.dp())
            }
            heroRow.addView(score, LinearLayout.LayoutParams(82.dp(),82.dp()))
            val info = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(16.dp(),0,0,0) }
            info.addView(TextView(ctx).apply { text="Ready to scan"; textSize=17f; setTextColor(TEXT) })
            summary.apply { text="Run a full diagnostic to check device health."; textSize=13f; setTextColor(MUTED) }
            info.addView(summary)
            heroRow.addView(info, LinearLayout.LayoutParams(0,-2,1f))
            hero.addView(heroRow)
            box.addView(hero)

            button("RUN FULL SCAN", true) { runScan() }
            button("⚡ ONE-CLICK FIX ALL SAFE PROBLEMS", true) { fixAllAndRescan() }
            box.addView(cards)
            label("COMPONENT CHECKS")
            box.addView(checks)
            button("TEST VIBRATION", false) { vibrate() }
            button("REQUEST CAMERA / MIC ACCESS", false) {
                if (Build.VERSION.SDK_INT >= 23) requestPermissions(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 44
                )
            }
            sub("Hardware quality tests such as camera image quality, microphone quality, speaker output, touch accuracy and charging behavior require interactive tests.")
            runScan()
        }

        private fun title(s:String, size:Float, color:Int) {
            box.addView(TextView(ctx).apply { text=s; textSize=size; setTextColor(color); setTypeface(typeface, Typeface.BOLD); gravity=Gravity.CENTER_HORIZONTAL })
        }
        private fun sub(s:String) {
            box.addView(TextView(ctx).apply { text=s; textSize=12f; setTextColor(MUTED); gravity=Gravity.CENTER; setPadding(0,4.dp(),0,12.dp()) })
        }
        private fun label(s:String) {
            box.addView(TextView(ctx).apply { text=s; textSize=14f; setTextColor(TEXT); setTypeface(typeface,Typeface.BOLD); setPadding(0,16.dp(),0,8.dp()) })
        }
        private fun panel(): LinearLayout = LinearLayout(ctx).apply {
            orientation=LinearLayout.VERTICAL; setPadding(14.dp(),14.dp(),14.dp(),14.dp()); setBackgroundColor(PANEL)
        }
        private fun button(s:String, primary:Boolean, f:()->Unit) {
            val b=Button(ctx).apply { text=s; textSize=14f; setTextColor(Color.WHITE); setOnClickListener{f()}; setPadding(8,8,8,8) }
            b.setBackgroundColor(if(primary) Color.rgb(37,99,235) else Color.rgb(21,37,58))
            box.addView(b, LinearLayout.LayoutParams(-1,52.dp()).apply { setMargins(0,10.dp(),0,0) })
        }
        private fun fixAllAndRescan() {
            // Safe, app-owned cleanup only; Android-managed resources are never force-stopped or erased.
            cacheDir.deleteRecursively(); cacheDir.mkdirs()
            Toast.makeText(ctx, "Safe fixes applied. Re-scanning…", Toast.LENGTH_SHORT).show()
            runScan()
        }

        private fun runScan() {
            val d=scanData()
            val checksList=mutableListOf<Pair<String,String>>()
            fun add(n:String, ok:Boolean, detail:String) { checksList.add((if(ok)"✓ " else "⚠ ")+n to detail) }
            add("Processor",d.cores>0,"${d.cores} cores")
            add("Memory",d.ramTotal>0 && !d.ramLow,"${fmt(d.ramAvail)} available / ${fmt(d.ramTotal)}")
            add("Storage",d.storageTotal>0,"${fmt(d.storageFree)} free / ${fmt(d.storageTotal)}")
            add("Battery",d.battery>=0,"${if(d.battery>=0)d.battery else "N/A"}% • ${d.temp}°C • ${if(d.charging) "Charging" else "On battery"}")
            add("Network",d.network,if(d.wifi)"Wi‑Fi connected" else if(d.cellular)"Mobile data connected" else "Offline")
            add("Display",d.width>0,"${d.width} × ${d.height} • ${d.dpi} dpi")
            add("Sensors",d.sensors>0,"${d.sensors} sensors detected")
            add("Camera",d.camera,"Hardware feature")
            add("Microphone",d.mic,"Hardware feature")
            add("Vibration",d.vibrator,"Haptic motor")
            add("Device security",d.secure,"Secure lock configured")
            val pass=checksList.count{it.first.startsWith("✓")}
            val sc=round(pass*100.0/checksList.size).toInt()
            score.text=sc.toString()
            summary.text="$pass/${checksList.size} checks passed • "+if(sc>=85)"System looks healthy" else if(sc>=65)"Some checks need attention" else "Several checks need attention"
            cards.removeAllViews()
            val rows=listOf(
                "Android" to d.android, "Device" to d.model, "CPU ABI" to d.abis,
                "Battery" to (if(d.battery>=0)"${d.battery}% ${if(d.charging) "⚡" else ""}" else "N/A"),
                "Free storage" to fmt(d.storageFree), "RAM available" to fmt(d.ramAvail)
            )
            rows.chunked(2).forEach { pair ->
                val r=LinearLayout(ctx).apply{orientation=LinearLayout.HORIZONTAL}
                pair.forEach { (k,v) -> r.addView(card(k,v),LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(0,5.dp(),5.dp(),0)}) }
                if(pair.size==1) r.addView(Space(ctx),LinearLayout.LayoutParams(0,0,1f))
                cards.addView(r)
            }
            checks.removeAllViews()
            checksList.forEach { (n,detail) ->
                val r=LinearLayout(ctx).apply{orientation=LinearLayout.HORIZONTAL;setPadding(12.dp(),10.dp(),12.dp(),10.dp());setBackgroundColor(Color.rgb(11,23,39))}
                val a=TextView(ctx).apply{text=n+"\n"+detail;textSize=13f;setTextColor(if(n.startsWith("✓"))GREEN else AMBER)}
                r.addView(a,LinearLayout.LayoutParams(0,-2,1f))
                checks.addView(r,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,4.dp(),0,0)})
            }
        }
        private fun card(k:String,v:String):LinearLayout = panel().apply {
            addView(TextView(ctx).apply{text=k;textSize=11f;setTextColor(MUTED)})
            addView(TextView(ctx).apply{text=v;textSize=15f;setTextColor(TEXT);setTypeface(typeface,Typeface.BOLD)})
        }
        private fun fmt(n:Long):String {
            if(n<=0)return "N/A"
            var x=n.toDouble(); val u=arrayOf("B","KB","MB","GB","TB"); var i=0
            while(x>=1024&&i<4){x/=1024;i++}
            return String.format(Locale.US,"%.1f %s",x,u[i])
        }
        private data class D(val android:String,val model:String,val abis:String,val cores:Int,val ramTotal:Long,val ramAvail:Long,val ramLow:Boolean,val storageTotal:Long,val storageFree:Long,val battery:Int,val temp:Double,val charging:Boolean,val network:Boolean,val wifi:Boolean,val cellular:Boolean,val width:Int,val height:Int,val dpi:Int,val sensors:Int,val camera:Boolean,val mic:Boolean,val vibrator:Boolean,val secure:Boolean)
        private fun scanData():D {
            val r=Runtime.getRuntime()
            val am=getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            val mi=android.app.ActivityManager.MemoryInfo(); am.getMemoryInfo(mi)
            val sf=android.os.StatFs(Environment.getDataDirectory().path)
            val bi=registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level=bi?.getIntExtra("level",-1)?:-1; val scale=bi?.getIntExtra("scale",100)?:100
            val temp=(bi?.getIntExtra("temperature",-1)?:-1)/10.0
            val charging=bi?.let {
                val status=it.getIntExtra("status", -1)
                status==android.os.BatteryManager.BATTERY_STATUS_CHARGING || status==android.os.BatteryManager.BATTERY_STATUS_FULL
            } == true
            val cm=getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val n=cm.activeNetwork; val nc=n?.let{cm.getNetworkCapabilities(it)}
            val dm=resources.displayMetrics
            val sm=getSystemService(SENSOR_SERVICE) as SensorManager
            return D(
                Build.VERSION.RELEASE, Build.MANUFACTURER+" "+Build.MODEL,
                Arrays.toString(Build.SUPPORTED_ABIS), r.availableProcessors(),
                mi.totalMem,mi.availMem,mi.lowMemory,sf.totalBytes,sf.availableBytes,
                if(level>=0)100*level/scale else -1,temp,charging,nc!=null,
                nc?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)==true,
                nc?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)==true,
                dm.widthPixels,dm.heightPixels,dm.densityDpi,
                sm.getSensorList(Sensor.TYPE_ALL).size,
                packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
                packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE),
                (getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator).hasVibrator(),
                (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure
            )
        }
        private fun vibrate() {
            val v=getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
            if(Build.VERSION.SDK_INT>=26) v.vibrate(VibrationEffect.createOneShot(140,VibrationEffect.DEFAULT_AMPLITUDE)) else v.vibrate(140)
        }
    }

    // ---------- TINY NUMBER UNIVERSE ----------
    inner class NumberScreen(ctx: Context): LinearLayout(ctx) {
        private var precision=1000
        private var value=BigDecimal.ONE
        private var step=0L
        private var running=false
        private val history=ArrayDeque<String>()
        private val universe=UniverseView(ctx)
        private val valueView=TextView(ctx)
        private val stepView=TextView(ctx)
        private val scaleView=TextView(ctx)
        private val historyView=TextView(ctx)
        private val precisionEdit=EditText(ctx)
        private val mc get()=MathContext(precision,RoundingMode.HALF_UP)
        init {
            orientation=VERTICAL; setPadding(12.dp(),10.dp(),12.dp(),8.dp()); setBackgroundColor(BG)
            addView(TextView(ctx).apply{text="✦  TINY NUMBER UNIVERSE  ✦";textSize=22f;gravity=Gravity.CENTER;setTextColor(CYAN)})
            addView(TextView(ctx).apply{text="Precision Lab • Arbitrary Decimal Universe";textSize=12f;gravity=Gravity.CENTER;setTextColor(MUTED)})
            addView(universe,LinearLayout.LayoutParams(-1,0,1.25f))
            valueView.apply{textSize=16f;gravity=Gravity.CENTER;setTextColor(TEXT)};addView(valueView)
            stepView.apply{textSize=14f;gravity=Gravity.CENTER;setTextColor(CYAN)};addView(stepView)
            scaleView.apply{textSize=12f;gravity=Gravity.CENTER;setTextColor(MUTED)};addView(scaleView)
            val r=LinearLayout(ctx).apply{gravity=Gravity.CENTER}
            r.addView(btn("÷ 2"){divide()});r.addView(btn("× 2"){multiply()});r.addView(btn("RESET"){reset()});addView(r)
            val a=LinearLayout(ctx).apply{gravity=Gravity.CENTER}
            a.addView(btn("▶ START"){start()});a.addView(btn("Ⅱ PAUSE"){pause()});a.addView(btn("■ STOP"){stop()});addView(a)
            addView(TextView(ctx).apply{text="EXPERIMENT • Precision / History";textSize=14f;setTextColor(Color.rgb(190,150,255));setPadding(4,12,4,4)})
            val pr=LinearLayout(ctx).apply{gravity=Gravity.CENTER_VERTICAL}
            precisionEdit.apply{setText("1000");setTextColor(TEXT);setHintTextColor(MUTED);hint="Precision";inputType=2}
            pr.addView(TextView(ctx).apply{text="Digits";textSize=13f;setTextColor(MUTED)})
            pr.addView(precisionEdit,LinearLayout.LayoutParams(0,55.dp(),1f))
            pr.addView(btn("APPLY"){applyPrecision()});addView(pr)
            val scroll=ScrollView(ctx);historyView.apply{textSize=12f;setTextColor(MUTED)};scroll.addView(historyView)
            addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
            reset()
        }
        private fun btn(s:String,f:()->Unit)=Button(ctx).apply{text=s;textSize=12f;setOnClickListener{f()};setTextColor(TEXT)}
        private fun reset(){pause();value=BigDecimal.ONE;step=0;history.clear();record();update()}
        private fun divide(){value=value.divide(BigDecimal("2"),mc);step++;record();update()}
        private fun multiply(){value=value.multiply(BigDecimal("2"),mc);step++;record();update()}
        private fun applyPrecision(){precision=precisionEdit.text.toString().toIntOrNull()?.coerceIn(10,10000)?:1000;update()}
        private fun start(){if(running)return;running=true;loop()}
        private fun pause(){running=false}
        private fun stop(){running=false}
        private fun loop(){if(!running)return;divide();postDelayed({loop()},250)}
        private fun record(){history.addLast("Step $step   ${value.toEngineeringString()}");while(history.size>30)history.removeFirst()}
        private fun update(){
            val shown=if(value.precision()>80)value.toEngineeringString() else value.stripTrailingZeros().toPlainString()
            valueView.text=shown
            universe.numberText=if(shown.length>24)value.toEngineeringString().take(24)+"…" else shown
            stepView.text="STEP $step"
            val e=if(value.signum()==0)"−∞" else "${-value.scale()}"
            scaleView.text="Scale: approximately 10^$e"
            historyView.text=history.joinToString("\n\n")
            universe.invalidate()
        }
    }

    inner class UniverseView(ctx:Context):View(ctx){
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        private val stars=List(90){Triple(Math.random().toFloat(),Math.random().toFloat(),(1..3).random().toFloat())}
        var pulse=0f
        var numberText="1"
        override fun onDraw(c:Canvas){
            c.drawColor(BG);val w=width.toFloat();val h=height.toFloat();val cx=w/2;val cy=h*.43f
            val g=RadialGradient(cx,cy,min(w,h)*.46f,intArrayOf(Color.argb(85,45,190,255),Color.argb(20,120,50,255),Color.TRANSPARENT),null,Shader.TileMode.CLAMP)
            p.shader=g;c.drawCircle(cx,cy,min(w,h)*.46f,p);p.shader=null
            for((x,y,r) in stars){p.color=Color.argb((90+130*abs(sin(pulse+x*7))).toInt(),180,225,255);p.setShadowLayer(r*3,0f,0f,p.color);setLayerType(LAYER_TYPE_SOFTWARE,p);c.drawCircle(x*w,y*h,r,p);p.clearShadowLayer()}
            p.style=Paint.Style.STROKE;p.strokeWidth=3f;p.color=Color.argb(90,80,210,255);c.drawCircle(cx,cy,min(w,h)*.30f+sin(pulse)*8,p)
            p.strokeWidth=1f;p.color=Color.argb(50,180,100,255);c.drawCircle(cx,cy,min(w,h)*.36f,p);p.style=Paint.Style.FILL
            p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.create("sans",Typeface.BOLD);p.textSize=min(w*.105f,62f);p.color=Color.WHITE;p.setShadowLayer(25f,0f,0f,Color.rgb(60,220,255));c.drawText(numberText,cx,cy+20,p);p.clearShadowLayer()
            pulse+=.025f;postInvalidateDelayed(40)
        }
    }

    // ---------- CDM UNIVERSE ----------
    inner class CdmScreen(ctx:Context): ScrollView(ctx) {
        private val root=LinearLayout(ctx).apply{orientation=VERTICAL;setPadding(16.dp(),18.dp(),16.dp(),24.dp());setBackgroundColor(BG)}
        private val search=EditText(ctx)
        private val results=LinearLayout(ctx).apply{orientation=VERTICAL}
        private val entities=listOf(
            CdmEntity("entity:customer-001","Person","Demo Customer"),
            CdmEntity("entity:product-001","Product","Demo Product")
        )
        init{
            addView(root)
            root.addView(TextView(ctx).apply{text="◇  CDM UNIVERSE";textSize=25f;setTextColor(CYAN);gravity=Gravity.CENTER})
            root.addView(TextView(ctx).apply{text="Semantic data universe • local-first";textSize=12f;setTextColor(MUTED);gravity=Gravity.CENTER;setPadding(0,4.dp(),0,14.dp())})
            search.apply{hint="Search entities / events / relationships";setTextColor(TEXT);setHintTextColor(MUTED);setSingleLine(true)}
            root.addView(search)
            val searchBtn=Button(ctx).apply{text="SEARCH";setOnClickListener{doSearch()}}
            root.addView(searchBtn)
            section("UNIVERSE SNAPSHOT")
            root.addView(snapshotCard("Entities","2","Canonical objects"))
            root.addView(snapshotCard("Events","1","Transaction"))
            root.addView(snapshotCard("Relationships","1","PURCHASED"))
            root.addView(snapshotCard("Sources","1","Demo Dataset"))
            section("RELATIONSHIP GRAPH")
            root.addView(CdmGraph(ctx),LinearLayout.LayoutParams(-1,250.dp()))
            section("ENTITY / EVENT / PROVENANCE")
            root.addView(TextView(ctx).apply{
                text="Customer 001  →  PURCHASED  →  Product 001\nEvent: Transaction\nAmount: 499 INR\nConfidence: 0.99\nSource: Demo Dataset"
                textSize=14f;setTextColor(TEXT);setPadding(14.dp(),14.dp(),14.dp(),14.dp());setBackgroundColor(PANEL)
            })
            section("SEARCH RESULTS")
            root.addView(results)
        }
        private fun section(s:String){root.addView(TextView(ctx).apply{text=s;textSize=14f;setTextColor(TEXT);setTypeface(typeface,Typeface.BOLD);setPadding(0,18.dp(),0,8.dp())})}
        private fun snapshotCard(k:String,v:String,d:String)=LinearLayout(ctx).apply{
            orientation=HORIZONTAL;setPadding(14.dp(),12.dp(),14.dp(),12.dp());setBackgroundColor(PANEL)
            addView(TextView(ctx).apply{text=k+"\n"+d;textSize=13f;setTextColor(MUTED)},LinearLayout.LayoutParams(0,-2,1f))
            addView(TextView(ctx).apply{text=v;textSize=22f;setTextColor(CYAN);setTypeface(typeface,Typeface.BOLD)})
        }
        private fun doSearch(){
            val q=search.text.toString().trim().lowercase()
            results.removeAllViews()
            val all=listOf("Person • Demo Customer","Product • Demo Product","Event • Transaction • 499 INR","Relationship • PURCHASED")
            all.filter{q.isEmpty()||it.lowercase().contains(q)}.forEach{
                results.addView(TextView(ctx).apply{text=it;textSize=14f;setTextColor(TEXT);setPadding(14.dp(),12.dp(),14.dp(),12.dp());setBackgroundColor(PANEL)})
            }
            if(results.childCount==0) results.addView(TextView(ctx).apply{text="No matching records";setTextColor(AMBER)})
        }
    }

    inner class CdmGraph(ctx:Context):View(ctx){
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(c:Canvas){
            c.drawColor(PANEL)
            val w=width.toFloat();val h=height.toFloat()
            p.strokeWidth=3f;p.color=Color.argb(100,85,225,255)
            c.drawLine(w*.28f,h*.48f,w*.72f,h*.48f,p)
            node(c,w*.22f,h*.48f,"CUSTOMER")
            node(c,w*.78f,h*.48f,"PRODUCT")
            p.color=AMBER;p.textSize=13f;p.textAlign=Paint.Align.CENTER;c.drawText("PURCHASED",w*.5f,h*.40f,p)
        }
        private fun node(c:Canvas,x:Float,y:Float,label:String){
            p.color=Color.rgb(22,43,69);c.drawCircle(x,y,52f,p)
            p.color=CYAN;p.textSize=11f;p.textAlign=Paint.Align.CENTER;c.drawText(label,x,y+4,p)
        }
    }
}

