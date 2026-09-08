package com.vibe.choreide.workspace

sealed class AospTemplate(
    val id: String,
    val displayName: String,
    val description: String,
    val fileName: String,
    val xml: String
) {
    object SystemUI : AospTemplate(
        id = "systemui",
        displayName = "SystemUI",
        description = "Status bar with clock, battery, and collapsed quick settings panel",
        fileName = "systemui_mockup.xml",
        xml = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#CC000000">

    <!-- Status Bar -->
    <com.android.systemui.statusbar.phone.PhoneStatusBarView
        android:layout_width="match_parent"
        android:layout_height="32dp"
        android:background="#99000000"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingStart="16dp"
        android:paddingEnd="16dp">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="12:00"
            android:textColor="#FFFFFF"
            android:textSize="14sp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="100%"
            android:textColor="#FFFFFF"
            android:textSize="14sp" />
    </com.android.systemui.statusbar.phone.PhoneStatusBarView>

    <!-- Quick Settings (collapsed) -->
    <com.android.systemui.qs.QSPanel
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="#B31A1A1A"
        android:padding="8dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="56dp"
            android:orientation="horizontal"
            android:gravity="center">

            <TextView
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:gravity="center"
                android:text="Wi-Fi"
                android:textColor="#FFFFFF" />

            <TextView
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:gravity="center"
                android:text="BT"
                android:textColor="#FFFFFF" />

            <TextView
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:gravity="center"
                android:text="Torch"
                android:textColor="#FFFFFF" />

            <TextView
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:gravity="center"
                android:text="DND"
                android:textColor="#FFFFFF" />
        </LinearLayout>
    </com.android.systemui.qs.QSPanel>
</LinearLayout>
"""
    )

    object Settings : AospTemplate(
        id = "settings",
        displayName = "Settings",
        description = "Scrollable settings list with dummy preference items",
        fileName = "settings_mockup.xml",
        xml = """<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#121212">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <com.android.settingslib.widget.MainSwitchPreference
            android:layout_width="match_parent"
            android:layout_height="64dp"
            android:background="#1E1E1E"
            android:gravity="center_vertical"
            android:padding="16dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Network &amp; internet"
                android:textColor="#FFFFFF"
                android:textSize="16sp" />
        </com.android.settingslib.widget.MainSwitchPreference>

        <com.android.settingslib.widget.MainSwitchPreference
            android:layout_width="match_parent"
            android:layout_height="64dp"
            android:layout_marginTop="8dp"
            android:background="#1E1E1E"
            android:gravity="center_vertical"
            android:padding="16dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Connected devices"
                android:textColor="#FFFFFF"
                android:textSize="16sp" />
        </com.android.settingslib.widget.MainSwitchPreference>

        <com.android.settingslib.widget.MainSwitchPreference
            android:layout_width="match_parent"
            android:layout_height="64dp"
            android:layout_marginTop="8dp"
            android:background="#1E1E1E"
            android:gravity="center_vertical"
            android:padding="16dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Display"
                android:textColor="#FFFFFF"
                android:textSize="16sp" />
        </com.android.settingslib.widget.MainSwitchPreference>

        <com.android.settingslib.widget.MainSwitchPreference
            android:layout_width="match_parent"
            android:layout_height="64dp"
            android:layout_marginTop="8dp"
            android:background="#1E1E1E"
            android:gravity="center_vertical"
            android:padding="16dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Battery"
                android:textColor="#FFFFFF"
                android:textSize="16sp" />
        </com.android.settingslib.widget.MainSwitchPreference>
    </LinearLayout>
</ScrollView>
"""
    )

    object Launcher : AospTemplate(
        id = "launcher",
        displayName = "Launcher",
        description = "Workspace with bottom hotseat container",
        fileName = "launcher_mockup.xml",
        xml = """<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#0A0A0A">

    <!-- Workspace -->
    <com.android.launcher3.Workspace
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_marginBottom="88dp" />

    <!-- Hotseat -->
    <com.android.launcher3.Hotseat
        android:layout_width="match_parent"
        android:layout_height="72dp"
        android:layout_gravity="bottom"
        android:background="#CC1A1A1A"
        android:orientation="horizontal"
        android:gravity="center">

        <TextView
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:layout_margin="6dp"
            android:background="#333333"
            android:gravity="center"
            android:text="App"
            android:textColor="#FFFFFF" />

        <TextView
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:layout_margin="6dp"
            android:background="#333333"
            android:gravity="center"
            android:text="App"
            android:textColor="#FFFFFF" />

        <TextView
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:layout_margin="6dp"
            android:background="#333333"
            android:gravity="center"
            android:text="App"
            android:textColor="#FFFFFF" />

        <TextView
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:layout_margin="6dp"
            android:background="#333333"
            android:gravity="center"
            android:text="App"
            android:textColor="#FFFFFF" />
    </com.android.launcher3.Hotseat>
</FrameLayout>
"""
    )

    object Empty : AospTemplate(
        id = "empty",
        displayName = "Empty",
        description = "Barebones FrameLayout canvas",
        fileName = "empty_mockup.xml",
        xml = """<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#121212" />
"""
    )

    companion object {
        val all = listOf(SystemUI, Settings, Launcher, Empty)
    }
}
