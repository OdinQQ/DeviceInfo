package com.quixom.apps.deviceinfo.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Camera
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Range
import android.util.Rational
import android.util.Size
import android.view.*
import android.widget.*
import com.quixom.apps.deviceinfo.R
import com.quixom.apps.deviceinfo.adapters.CameraAdapter
import com.quixom.apps.deviceinfo.models.FeaturesHW
import com.quixom.apps.deviceinfo.utilities.KeyUtil
import java.util.*


class CameraFragment : BaseFragment(), View.OnClickListener {

    @RequiresApi(Build.VERSION_CODES.M)
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onClick(view: View?) {
        when (view) {
            tvRearCamera -> {
                checkCameraPermission("1")
                tabSelector(tvRearCamera!!, tvFrontCamera!!)
            }
            tvFrontCamera -> {
                checkCameraPermission("0")
                tabSelector(tvFrontCamera!!, tvRearCamera!!)
            }
        }
    }

    var ivMenu: ImageView? = null
    var ivBack: ImageView? = null
    var tvTitle: TextView? = null

    var tvCameraFeature: TextView? = null
    var tvRearCamera: TextView? = null
    var tvFrontCamera: TextView? = null
    var textAreaScroller: ScrollView? = null
    var llParentCamera: LinearLayout? = null
    private var rvCameraFeatures: RecyclerView? = null

    var camera: Camera? = null
    // 相机管理器
    private var cameraManager: CameraManager? = null

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        val view = inflater.inflate(R.layout.fragment_camera, container, false)
        // 更改样式
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.CameraTheme)
        val localInflater = inflater.cloneInContext(contextThemeWrapper)
        val view = localInflater.inflate(R.layout.fragment_camera, container, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = activity!!.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = resources.getColor(R.color.dark_green_blue)
            window.navigationBarColor = resources.getColor(R.color.dark_green_blue)
        }

        ivMenu = view.findViewById(R.id.iv_menu)
        ivBack = view.findViewById(R.id.iv_back)
        tvTitle = view.findViewById(R.id.tv_title)
        tvCameraFeature = view.findViewById(R.id.tv_camera_feature)
        tvRearCamera = view.findViewById(R.id.tv_rear_camera)
        tvFrontCamera = view.findViewById(R.id.tv_front_camera)
        textAreaScroller = view.findViewById(R.id.textAreaScroller)
        llParentCamera = view.findViewById(R.id.ll_parent_camera)
        rvCameraFeatures = view.findViewById(R.id.rv_Camera_Features)
        cameraManager = mActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager?
        camera = Camera()
        return view
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initToolbar()
        tvRearCamera?.setOnClickListener(this)
        tvFrontCamera?.setOnClickListener(this)
        if (cameraManager?.cameraIdList?.size!! >= 2) {
            llParentCamera?.visibility = View.VISIBLE
        } else {
            llParentCamera?.visibility = View.GONE
        }

        rvCameraFeatures?.layoutManager = LinearLayoutManager(mActivity)
        rvCameraFeatures?.hasFixedSize()

        checkCameraPermission("1")
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && isAdded) {
            initToolbar()
        }
    }

    private fun initToolbar() {
        ivMenu?.visibility = View.VISIBLE
        ivBack?.visibility = View.GONE
        tvTitle?.text = mResources.getString(R.string.camera)
        ivMenu?.setOnClickListener {
            mActivity.openDrawer()
        }
    }

    /**
     * this method will show permission pop up messages to user.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun checkCameraPermission(ids: String) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasWriteCameraPermission = mActivity.checkSelfPermission(Manifest.permission.CAMERA)
            if (hasWriteCameraPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), KeyUtil.KEY_CAMERA_CODE)
            } else {
                fetchCameraCharacteristics(cameraManager!!, ids)
            }
        } else {
            fetchCameraCharacteristics(cameraManager!!, ids)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            KeyUtil.KEY_CAMERA_CODE -> if (permissions.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 获取权限
                    fetchCameraCharacteristics(cameraManager!!, "1")
                } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.GET_ACCOUNTS)) {
                        //Show permission explanation dialog...
                        Toast.makeText(mActivity, "Need to grant account Permission", Toast.LENGTH_LONG).show()
                    }
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    /**
     * 切换 Tab 样式
     */
    private fun tabSelector(textview1: TextView, textview2: TextView) {
        /*** Set text color */
        textview1.setTextColor(ContextCompat.getColor(mActivity, R.color.font_white))
        textview2.setTextColor(ContextCompat.getColor(mActivity, R.color.orange))

        /*** Background color */
        textview1.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.orange))
        textview2.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.font_white))

        /*** Set background drawable */
        textview1.setBackgroundResource(R.drawable.rectangle_fill)
        textview2.setBackgroundResource(R.drawable.rectangle_unfill)
    }

    /**
     * 获取相机特性
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun fetchCameraCharacteristics(cameraManager: CameraManager, ids: String) {
        val lists = ArrayList<FeaturesHW>()
        val sb = StringBuilder()
        val characteristics = cameraManager.getCameraCharacteristics(ids)
        for (key in characteristics.keys) {
            sb.append(key.name).append("=").append(getCharacteristicsValue(key, characteristics)).append("\n\n")
            val keyNm = key.name.split(".")
            if (getCharacteristicsValue(key, characteristics) != "") {
                if (key.name.split(".").size == 4) {
                    lists.add(FeaturesHW(keyNm[3], getCharacteristicsValue(key, characteristics)))
                } else {
                    lists.add(FeaturesHW(keyNm[2], getCharacteristicsValue(key, characteristics)))
                }
            }
        }
        //
        val adapter = CameraAdapter(lists, mActivity)
        //now adding the adapter to RecyclerView
        rvCameraFeatures?.adapter = adapter
    }

    /**
     * 获得属性值
     */
    @SuppressLint("NewApi")
    @Suppress("UNCHECKED_CAST")
    private fun <T> getCharacteristicsValue(key: CameraCharacteristics.Key<T>, characteristics: CameraCharacteristics): String {
        val values = mutableListOf<String>()
        when (key) {
            CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES -> {
                val modes = characteristics.get(key) as IntArray
                modes.forEach {
                    when (it) {
                        CameraCharacteristics.COLOR_CORRECTION_ABERRATION_MODE_OFF -> values.add("Off")
                        CameraCharacteristics.COLOR_CORRECTION_ABERRATION_MODE_FAST -> values.add("Fast")
                        CameraCharacteristics.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY -> values.add("High Quality")
                    }
                }
            }
            CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES -> {
                val modes = characteristics.get(key) as IntArray
                modes.forEach {
                    when (it) {
                        CameraCharacteristics.CONTROL_AE_ANTIBANDING_MODE_OFF -> values.add("Off")
                        CameraCharacteristics.CONTROL_AE_ANTIBANDING_MODE_AUTO -> values.add("Auto")
                        CameraCharacteristics.CONTROL_AE_ANTIBANDING_MODE_50HZ -> values.add("50Hz")
                        CameraCharacteristics.CONTROL_AE_ANTIBANDING_MODE_60HZ -> values.add("60Hz")
                    }
                }
            }
            CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES -> {
                val modes = characteristics.get(key) as IntArray
                modes.forEach {
                    when (it) {
                        CameraCharacteristics.CONTROL_AE_MODE_OFF -> values.add("Off")
                        CameraCharacteristics.CONTROL_AE_MODE_ON -> values.add("On")
                        CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH -> values.add("Always Flash")
                        CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH -> values.add("Auto Flash")
                        CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE -> values.add("Auto Flash Redeye")
                    }
                }
            }
            CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES -> {
                val ranges = characteristics.get(key) as Array<Range<Int>>
                ranges.forEach {
                    values.add(getRangeValue(it))
                }
            }
            CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE -> {
                val range = characteristics.get(key) as Range<Int>
                values.add(getRangeValue(range))
            }
            CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP -> {
                val step = characteristics.get(key) as Rational
                values.add(step.toString())
            }/* else if (CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE == key) { // TODO requires >23
            val available = characteristics.get(key)
            values.add(available.toString())
        } */
            CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES -> {
                val modes = characteristics.get(key) as IntArray
                modes.forEach {
                    when (it) {
                        CameraCharacteristics.CONTROL_AF_MODE_OFF -> values.add("Off")
                        CameraCharacteristics.CONTROL_AF_MODE_AUTO -> values.add("Auto")
                        CameraCharacteristics.CONTROL_AF_MODE_EDOF -> values.add("EDOF")
                        CameraCharacteristics.CONTROL_AF_MODE_MACRO -> values.add("Macro")
                        CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> values.add("Continous Picture")
                        CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> values.add("Continous Video")
                    }
                }
            }
            CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS -> {
                val effects = characteristics.get(key) as IntArray
                effects.forEach {
                    values.add(when (it) {
                        CameraCharacteristics.CONTROL_EFFECT_MODE_OFF -> "Off"
                        CameraCharacteristics.CONTROL_EFFECT_MODE_AQUA -> "Aqua"
                        CameraCharacteristics.CONTROL_EFFECT_MODE_BLACKBOARD -> "Blackboard"
                        CameraCharacteristics.CONTROL_EFFECT_MODE_MONO -> "Mono"
                        CameraCharacteristics.CONTROL_EFFECT_MODE_NEGATIVE -> "Negative"
                        CameraCharacteristics.CONTROL_EFFECT_MODE_POSTERIZE -> "Posterize"
                        CameraCharacteristics.CONTROL_EFFECT_MODE_SEPIA -> "Sepia"
                        CameraCharacteristics.CONTROL_EFFECT_MODE_SOLARIZE -> "Solarize"
                        CameraCharacteristics.CONTROL_EFFECT_MODE_WHITEBOARD -> "Whiteboard"
                        else -> {
                            "Unkownn ${it}"
                        }
                    })
                }
            } /*else if (CameraCharacteristics.CONTROL_AVAILABLE_MODES == key) {
            val modes = characteristics.get(key) as IntArray
            modes.forEach {
                values.add(when (it) {
                    CameraCharacteristics.CONTROL_MODE_OFF -> "Off"
                    CameraCharacteristics.CONTROL_MODE_OFF_KEEP_STATE -> "Off Keep State"
                    CameraCharacteristics.CONTROL_MODE_AUTO -> "Auto"
                    CameraCharacteristics.CONTROL_MODE_USE_SCENE_MODE -> "Use Scene Mode"
                    else -> {
                        "Unkownn ${it}"
                    }
                })
            }
        }*/
            CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES -> {
                val modes = characteristics.get(key) as IntArray
                modes.forEach {
                    values.add(when (it) {
                        CameraCharacteristics.CONTROL_SCENE_MODE_DISABLED -> "Disabled"
                        CameraCharacteristics.CONTROL_SCENE_MODE_ACTION -> "Action"
                        CameraCharacteristics.CONTROL_SCENE_MODE_BARCODE -> "Barcode"
                        CameraCharacteristics.CONTROL_SCENE_MODE_BEACH -> "Beach"
                        CameraCharacteristics.CONTROL_SCENE_MODE_CANDLELIGHT -> "Candlelight"
                        CameraCharacteristics.CONTROL_SCENE_MODE_FACE_PRIORITY -> "Face Priority"
                        CameraCharacteristics.CONTROL_SCENE_MODE_FIREWORKS -> "Fireworks"
                        CameraCharacteristics.CONTROL_SCENE_MODE_HDR -> "HDR"
                        CameraCharacteristics.CONTROL_SCENE_MODE_LANDSCAPE -> "Landscape"
                        CameraCharacteristics.CONTROL_SCENE_MODE_NIGHT -> "Night"
                        CameraCharacteristics.CONTROL_SCENE_MODE_NIGHT_PORTRAIT -> "Night Portrait"
                        CameraCharacteristics.CONTROL_SCENE_MODE_PARTY -> "Party"
                        CameraCharacteristics.CONTROL_SCENE_MODE_PORTRAIT -> "Portrait"
                        CameraCharacteristics.CONTROL_SCENE_MODE_SNOW -> "Snow"
                        CameraCharacteristics.CONTROL_SCENE_MODE_SPORTS -> "Sports"
                        CameraCharacteristics.CONTROL_SCENE_MODE_STEADYPHOTO -> "Steady Photo"
                        CameraCharacteristics.CONTROL_SCENE_MODE_SUNSET -> "Sunset"
                        CameraCharacteristics.CONTROL_SCENE_MODE_THEATRE -> "Theatre"
                        CameraCharacteristics.CONTROL_SCENE_MODE_HIGH_SPEED_VIDEO -> "High Speed Video"
                        else -> {
                            "Unkownn ${it}"
                        }
                    })
                }
            }
            CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES -> {
                val modes = characteristics.get(key) as IntArray
                modes.forEach {
                    values.add(when (it) {
                        CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON -> "On"
                        CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF -> "Off"
                        else -> {
                            "Unkownn ${it}"
                        }
                    })
                }
            }
            CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES -> {

            } /*else if (CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE == key) {

        } */
            CameraCharacteristics.CONTROL_MAX_REGIONS_AE -> {

            }
            CameraCharacteristics.CONTROL_MAX_REGIONS_AF -> {

            }
            CameraCharacteristics.CONTROL_MAX_REGIONS_AWB -> {

            } /*else if (CameraCharacteristics.CONTROL_POST_RAW_SENSITIVITY_BOOST_RANGE == key) {

        } else if (CameraCharacteristics.DEPTH_DEPTH_IS_EXCLUSIVE == key) {

        }*/
            CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES -> {

            }
            CameraCharacteristics.FLASH_INFO_AVAILABLE -> {

            }
            CameraCharacteristics.HOT_PIXEL_AVAILABLE_HOT_PIXEL_MODES -> {

            }
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL -> {

            }
            CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES -> {

            }
            CameraCharacteristics.LENS_FACING -> {
                val facing = characteristics.get(key)
                values.add(
                        when (facing) {
                            CameraCharacteristics.LENS_FACING_BACK -> "Back"
                            CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                            else -> "Unkown"
                        }
                )
            }
            CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES -> {

            }
            CameraCharacteristics.LENS_INFO_AVAILABLE_FILTER_DENSITIES -> {

            }
            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS -> {

            }
            CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION -> {

            }
            CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION -> {

            }
            CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE -> {

            }
            CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE -> {

            }/* else if (CameraCharacteristics.LENS_INTRINSIC_CALIBRATION == key) {

        } else if (CameraCharacteristics.LENS_POSE_ROTATION == key) {

        } /**/else if (CameraCharacteristics.LENS_POSE_TRANSLATION == key) {

        } /**/else if (CameraCharacteristics.LENS_RADIAL_DISTORTION == key) {

        }*//* else if (CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES == key) {

        } *//*else if (CameraCharacteristics.REPROCESS_MAX_CAPTURE_STALL == key) {

        } */
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES -> {

            }
            CameraCharacteristics.REQUEST_MAX_NUM_INPUT_STREAMS -> {

            }
            CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_PROC -> {

            }
            CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_PROC_STALLING -> {

            }
            CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_RAW -> {

            }
            CameraCharacteristics.REQUEST_PARTIAL_RESULT_COUNT -> {

            }
            CameraCharacteristics.REQUEST_PIPELINE_MAX_DEPTH -> {

            }
            CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM -> {

            }
            CameraCharacteristics.SCALER_CROPPING_TYPE -> {

            }
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP -> {
                val map = characteristics.get(key) as StreamConfigurationMap
                val outputFormats = map.outputFormats
                outputFormats.forEach {
                    values.add(printOutputFormat(it, map))
                }
            }
            CameraCharacteristics.SENSOR_AVAILABLE_TEST_PATTERN_MODES -> {

            }
            CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN -> {

            }
            CameraCharacteristics.SENSOR_CALIBRATION_TRANSFORM1 -> {

            }
            CameraCharacteristics.SENSOR_CALIBRATION_TRANSFORM2 -> {

            }
            CameraCharacteristics.SENSOR_COLOR_TRANSFORM1 -> {

            }
            CameraCharacteristics.SENSOR_COLOR_TRANSFORM2 -> {

            }
            CameraCharacteristics.SENSOR_FORWARD_MATRIX1 -> {

            }
            CameraCharacteristics.SENSOR_FORWARD_MATRIX2 -> {

            }
            CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE -> {

            }
            CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT -> {

            }
            CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE -> {

            } /*else if (CameraCharacteristics.SENSOR_INFO_LENS_SHADING_APPLIED == key) {

        } */
            CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION -> {

            }
            CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE -> {

            }
            CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE -> {

            } /*else if (CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE == key) {

        }*/
            CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE -> {

            }
            CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE -> {

            }
            CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL -> {

            }
            CameraCharacteristics.SENSOR_MAX_ANALOG_SENSITIVITY -> {

            } /*else if (CameraCharacteristics.SENSOR_OPTICAL_BLACK_REGIONS == key) {

        }*/
            CameraCharacteristics.SENSOR_ORIENTATION -> {
                val orientation = characteristics.get(key) as Int
                values.add(orientation.toString())

            }
            CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1 -> {

            }
            CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT2 -> {

            }/* else if (CameraCharacteristics.SHADING_AVAILABLE_MODES == key) {

        }*/
            CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES -> {

            }
            CameraCharacteristics.STATISTICS_INFO_AVAILABLE_HOT_PIXEL_MAP_MODES -> {

            }/* else if (CameraCharacteristics.STATISTICS_INFO_AVAILABLE_LENS_SHADING_MAP_MODES == key) {

        }*/
            CameraCharacteristics.SYNC_MAX_LATENCY -> {

            }
            CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES -> {

            }
            CameraCharacteristics.TONEMAP_MAX_CURVE_POINTS -> {

            }
        }
        values.sort()
        return if (values.isEmpty()) "" else join(", ", values)
    }

    /**
     * 打印输出格式
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun printOutputFormat(outputFormat: Int, map: StreamConfigurationMap): String {
        val formatName = getImageFormat(outputFormat)
        val outputSizes = map.getOutputSizes(outputFormat);
        val outputSizeValues = mutableListOf<String>()
        outputSizes.forEach {
            val mp = getMegaPixel(it)
            outputSizeValues.add("${it.width}x${it.height} (${mp}MP)")
        }
        val sizesString = join(", ", outputSizeValues)
        return "\n$formatName -> [$sizesString]"
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getMegaPixel(size: Size): String {
        val mp = (size.width * size.height) / 1000000f
        return String.format("%.1f", mp)
    }

    /**
     * 获取图片格式
     */
    private fun getImageFormat(format: Int): String {
        return when (format) {
            ImageFormat.DEPTH16 -> "DEPTH16"
            ImageFormat.DEPTH_POINT_CLOUD -> "DEPTH_POINT_CLOUD"
            ImageFormat.FLEX_RGBA_8888 -> "FLEX_RGBA_8888"
            ImageFormat.FLEX_RGB_888 -> "FLEX_RGB_888"
            ImageFormat.JPEG -> "JPEG"
            ImageFormat.NV16 -> "NV16"
            ImageFormat.NV21 -> "NV21"
            ImageFormat.PRIVATE -> "PRIVATE"
            ImageFormat.RAW10 -> "RAW10"
            ImageFormat.RAW12 -> "RAW12"
            ImageFormat.RAW_PRIVATE -> "RAW_PRIVATE"
            ImageFormat.RAW_SENSOR -> "RAW_SENSOR"
            ImageFormat.RGB_565 -> "RGB_565"
            ImageFormat.YUV_420_888 -> "YUV_420_888"
            ImageFormat.YUV_422_888 -> "YUV_422_888"
            ImageFormat.YUV_444_888 -> "YUV_444_888"
            ImageFormat.YUY2 -> "YUY2"
            ImageFormat.YV12 -> "YV12"
            else -> "Unkown"
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun <T : Comparable<T>> getRangeValue(range: Range<T>): String {
        return "[${range.lower},${range.upper}]"
    }

    fun <T> join(delimiter: String, elements: Collection<T>?): String {
        if (null == elements || elements.isEmpty()) {
            return ""
        }
        val sb = StringBuilder()
        val iter = elements.iterator()
        while (iter.hasNext()) {
            val element = iter.next()
            sb.append(element.toString())
            if (iter.hasNext()) {
                sb.append(delimiter)
            }
        }
        return sb.toString()
    }
}


