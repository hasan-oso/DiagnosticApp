-- ============================================================================
-- Supabase Database Schema for Android Diagnostic App
-- ============================================================================
-- Instructions:
-- 1. Go to your Supabase project → SQL Editor
-- 2. Copy and paste this entire file
-- 3. Click "Run" to execute
-- ============================================================================

-- Create the main device_scans table
CREATE TABLE IF NOT EXISTS device_scans (
    -- Primary Key
    scan_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Metadata (4 fields)
    scan_timestamp BIGINT NOT NULL,
    scan_duration_seconds INTEGER NOT NULL,
    app_version TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Identity (12 fields)
    device_brand TEXT,
    device_model TEXT,
    device_manufacturer TEXT,
    board_platform TEXT,
    build_fingerprint TEXT,
    build_id TEXT,
    build_date_utc BIGINT,
    android_version TEXT,
    android_sdk INTEGER,
    kernel_version TEXT,
    security_patch_date TEXT,
    build_tags TEXT,
    
    -- Security (9 fields)
    play_services_available BOOLEAN,
    play_integrity_verdict TEXT,
    safety_net_basic_integrity BOOLEAN,
    unknown_sources_count INTEGER,
    developer_options_enabled BOOLEAN,
    adb_enabled BOOLEAN,
    usb_debugging_enabled BOOLEAN,
    device_admin_count INTEGER,
    verified_boot_state TEXT,
    
    -- CPU/GPU (10 fields)
    cpu_architecture TEXT,
    supported_abis TEXT[], -- Array of strings
    cpu_core_count INTEGER,
    cpu_max_freq_mhz INTEGER,
    cpu_min_freq_mhz INTEGER,
    gpu_renderer TEXT,
    gpu_vendor TEXT,
    opengl_version TEXT,
    vulkan_version TEXT,
    npu_available BOOLEAN,
    
    -- Memory/Storage (8 fields)
    ram_total_mb BIGINT,
    ram_available_mb BIGINT,
    ram_low_threshold_mb BIGINT,
    storage_total_gb BIGINT,
    storage_available_gb BIGINT,
    external_storage_available BOOLEAN,
    adoptable_storage_enabled BOOLEAN,
    storage_emulated BOOLEAN,
    
    -- Display (8 fields)
    screen_width_px INTEGER,
    screen_height_px INTEGER,
    screen_density_dpi INTEGER,
    screen_refresh_rate_hz FLOAT,
    screen_refresh_rates_supported FLOAT[], -- Array of floats
    display_hdr_capabilities TEXT[], -- Array of strings
    wide_color_gamut_support BOOLEAN,
    screen_brightness_mode TEXT,
    
    -- Camera/Sensors (7 fields)
    camera_count INTEGER,
    back_camera_resolution_mp FLOAT,
    front_camera_resolution_mp FLOAT,
    camera_flash_available BOOLEAN,
    camera_ois_available BOOLEAN,
    sensor_count_total INTEGER,
    sensor_types_list TEXT[], -- Array of strings
    
    -- Battery (6 fields)
    battery_design_capacity_mah INTEGER,
    battery_health_status TEXT,
    battery_technology TEXT,
    battery_temp_celsius FLOAT,
    battery_voltage_mv INTEGER,
    battery_current_now_ma INTEGER,
    
    -- Connectivity (7 fields)
    wifi_available BOOLEAN,
    bluetooth_version TEXT,
    nfc_available BOOLEAN,
    mobile_network_type TEXT,
    dual_sim_support BOOLEAN,
    esim_support BOOLEAN,
    usb_version TEXT,
    
    -- Performance/Thermal (13 fields)
    system_uptime_hours FLOAT,
    running_processes_count INTEGER,
    running_services_count INTEGER,
    installed_packages_count INTEGER,
    system_apps_count INTEGER,
    user_apps_count INTEGER,
    cpu_temp_celsius FLOAT,
    thermal_throttling_status TEXT,
    cpu_usage_percent FLOAT,
    memory_pressure_level TEXT,
    anr_count_recent INTEGER,
    crash_count_recent INTEGER,
    bootloop_indicator BOOLEAN,
    
    -- Device Age (2 fields)
    device_age_years FLOAT,
    device_age_category TEXT,
    
    -- CPU Stress Test Results (nested as JSONB)
    cpu_stress_result JSONB,
    -- Structure:
    -- {
    --   "durationSeconds": 480,
    --   "maxTempCelsius": 65.5,
    --   "throttlingStartSec": 120,
    --   "performanceDegradationPercent": 15.5,
    --   "singleCoreScore": 2500,
    --   "multiCoreScore": 8000,
    --   "tempReadings": [{"sec": 0, "temp": 35.0}, ...],
    --   "freqReadings": [{"sec": 0, "freqMhz": 2400}, ...]
    -- }
    
    -- Storage Stress Test Results (nested as JSONB)
    storage_stress_result JSONB,
    -- Structure:
    -- {
    --   "sequentialWriteMbps": 450.5,
    --   "sequentialReadMbps": 650.2,
    --   "random4kReadIops": 15000,
    --   "random4kWriteIops": 12000,
    --   "slcCacheCrash": false,
    --   "detectedType": "UFS_3_1",
    --   "writeSpeedCurve": [{"fileNum": 1, "speedMbps": 500.0}, ...]
    -- }
    
    -- Battery Stress Test Results (nested as JSONB)
    battery_stress_result JSONB,
    -- Structure:
    -- {
    --   "durationSeconds": 240,
    --   "initialVoltageMv": 4200,
    --   "loadVoltageMv": 4050,
    --   "voltageSagMv": 150,
    --   "peakCurrentMa": 3500,
    --   "internalResistanceMohm": 42.8,
    --   "estimatedHealthPercent": 90,
    --   "voltageReadings": [{"sec": 0, "voltageMv": 4200}, ...]
    -- }
    
    -- Human Labels (will be updated after expert review)
    human_labels JSONB
    -- Structure:
    -- {
    --   "overallHealthScore": 85,
    --   "authenticityVerdict": "GENUINE",
    --   "refurbishmentStatus": "ORIGINAL",
    --   "usageIntensity": "NORMAL",
    --   "replacedComponents": ["battery", "screen"],
    --   "knownIssues": ["minor_scratch_on_back"],
    --   "technicianNotes": "Device in good condition",
    --   "labeledAt": 1707667200000,
    --   "technicianId": "expert_123"
    -- }
);

-- Create indexes for faster queries
CREATE INDEX idx_device_scans_timestamp ON device_scans(scan_timestamp DESC);
CREATE INDEX idx_device_scans_model ON device_scans(device_model);
CREATE INDEX idx_device_scans_manufacturer ON device_scans(device_manufacturer);
CREATE INDEX idx_device_scans_created_at ON device_scans(created_at DESC);

-- Create index on JSONB fields for faster queries
CREATE INDEX idx_cpu_stress_score ON device_scans((cpu_stress_result->>'multiCoreScore'));
CREATE INDEX idx_storage_type ON device_scans((storage_stress_result->>'detectedType'));
CREATE INDEX idx_human_labels_health ON device_scans((human_labels->>'overallHealthScore'));

-- Enable Row Level Security (RLS)
ALTER TABLE device_scans ENABLE ROW LEVEL SECURITY;

-- Create policy to allow public inserts (for the app)
CREATE POLICY "Allow public inserts" 
ON device_scans 
FOR INSERT 
TO anon 
WITH CHECK (true);

-- Create policy to allow public reads (for analytics/ML)
CREATE POLICY "Allow public reads" 
ON device_scans 
FOR SELECT 
TO anon 
USING (true);

-- Create policy to allow public updates (for labeling)
CREATE POLICY "Allow public updates" 
ON device_scans 
FOR UPDATE 
TO anon 
USING (true)
WITH CHECK (true);

-- Create a view for quick statistics
CREATE OR REPLACE VIEW device_scan_stats AS
SELECT 
    device_manufacturer,
    device_model,
    COUNT(*) as scan_count,
    AVG((human_labels->>'overallHealthScore')::INTEGER) as avg_health_score,
    AVG(device_age_years) as avg_age_years,
    AVG((cpu_stress_result->>'maxTempCelsius')::FLOAT) as avg_max_temp,
    MODE() WITHIN GROUP (ORDER BY (storage_stress_result->>'detectedType')) as most_common_storage
FROM device_scans
WHERE human_labels IS NOT NULL
GROUP BY device_manufacturer, device_model
ORDER BY scan_count DESC;

-- Grant access to the view
GRANT SELECT ON device_scan_stats TO anon;

-- ============================================================================
-- Done! Your database is ready.
-- ============================================================================
-- Next steps:
-- 1. Go to Settings → API to get your SUPABASE_URL and SUPABASE_ANON_KEY
-- 2. Update Constants.kt with these values
-- 3. Build and run the app!
-- ============================================================================
