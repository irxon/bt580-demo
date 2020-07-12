package com.irxon.bt580.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.irxon.bt580.R;
import com.irxon.bt580.service.BluetoothLeService;

/**
 * 编程说明：这个JAVA文件里的代码，是蓝牙BLE应用开发的主要代码。它的主要功能包括：与BT580从机建立蓝牙连接、查找BT580串口透传的特征值、
 * 发送文本对特征值进行写操作、开启特征值的Notify通知功能、监听Notify通知的数据并在屏幕上方显示。
 * 通过对特征值的写和监听，实现与BT580的蓝牙BLE通讯，进而与BT580所连接的设备实现无线串口通讯。
 * BT580用户的个性化应用开发，只需要改变数据处理方式即可。
 * 本例程将收到的数据直接在手机屏幕上方显示，用户可根据自己串口设备的特点，修改一下界面，处理一下数据，然后再显示出来。
 * 本例程将文本直接写特征值，用户可根据自己串口设备的特点，修改一下界面，比如设置多个按钮，实现对串口设备的远程控制或设置。
 */

public class Ble_Activity extends Activity implements OnClickListener {

	private final static String TAG = Ble_Activity.class.getSimpleName();
	public static String HEART_RATE_MEASUREMENT = "0000fff6-0000-1000-8000-00805f9b34fb";  //BT580串口透传特征值
	public static String EXTRAS_DEVICE_NAME = "DEVICE_NAME";;
	public static String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	public static String EXTRAS_DEVICE_RSSI = "RSSI";
	//蓝牙连接状态
	private boolean mConnected = false;
	private String status = "disconnected";
	//蓝牙名字
	private String mDeviceName;
	//蓝牙地址
	private String mDeviceAddress;
	//蓝牙信号值
	private String mRssi;
	private Bundle b;
	private String rev_str = "";
	//蓝牙service,负责后台的蓝牙服务
	private static BluetoothLeService mBluetoothLeService;
	//文本框，显示接受的内容
	private TextView rev_tv, connect_state;
	//发送按钮
	private Button send_btn;
	//文本编辑框
	private EditText send_et;
	private ScrollView rev_sv;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	//蓝牙特征值
	private static BluetoothGattCharacteristic target_chara = null;
	private Handler mhandler = new Handler();
	private Handler myHandler = new Handler()
	{
		// 2.��д��Ϣ������
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			//判断发送的消息
			case 1:
			{
				// 更新View
				String state = msg.getData().getString("connect_state");
				connect_state.setText(state);

				break;
			}

			}
			super.handleMessage(msg);
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ble_activity);
		b = getIntent().getExtras();
		//从意图获取显示的蓝牙信息
		mDeviceName = b.getString(EXTRAS_DEVICE_NAME);
		mDeviceAddress = b.getString(EXTRAS_DEVICE_ADDRESS);
		mRssi = b.getString(EXTRAS_DEVICE_RSSI);

		/* 启动蓝牙service */
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
		init();

	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		//解除广播接收器
		unregisterReceiver(mGattUpdateReceiver);
		mBluetoothLeService = null;
	}

	// Activity出来时候，绑定广播接收器，监听蓝牙连接服务传过来的事件
	@Override
	protected void onResume()
	{
		super.onResume();
		//绑定广播接收器
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (mBluetoothLeService != null)
		{    
			//根据蓝牙地址，建立连接
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
			Log.d(TAG, "Connect request result=" + result);
		}
	}

	/** 
	* @Title: init 
	* @Description: TODO(初始化UI控件)
	* @param  ��
	* @return void    
	* @throws 
	*/ 
	private void init()
	{
		rev_sv = (ScrollView) this.findViewById(R.id.rev_sv);
		rev_tv = (TextView) this.findViewById(R.id.rev_tv);
		connect_state = (TextView) this.findViewById(R.id.connect_state);
		send_btn = (Button) this.findViewById(R.id.send_btn);
		send_et = (EditText) this.findViewById(R.id.send_et);
		connect_state.setText(status);
		send_btn.setOnClickListener(this);

	}

	/* BluetoothLeService绑定的回调函数 */
	private final ServiceConnection mServiceConnection = new ServiceConnection()
	{

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service)
		{
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize())
			{
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up
			// initialization.
			// 根据蓝牙地址，连接设备
			mBluetoothLeService.connect(mDeviceAddress);

		}

		@Override
		public void onServiceDisconnected(ComponentName componentName)
		{
			mBluetoothLeService = null;
		}

	};

	/**
	 * 广播接收器，负责接收BluetoothLeService类发送的数据
	 */

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))    //连接成功
			{
				mConnected = true;
				status = "connected";
				//更新连接状态
				updateConnectionState(status);
				System.out.println("BroadcastReceiver :" + "device connected");

			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED    //连接失败
					.equals(action))
			{
				mConnected = false;
				status = "disconnected";
				//更新连接状态
				updateConnectionState(status);
				System.out.println("BroadcastReceiver :"+ "device disconnected");

			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED      //如发现有蓝牙T服务
					.equals(action))
			{
				// Show all the supported services and characteristics on the user interface.
				//查找所有蓝牙服务和特征值，找到BT580的透传特征值
				displayGattServices(mBluetoothLeService.getSupportedGattServices());
				System.out.println("BroadcastReceiver :"+ "device SERVICES_DISCOVERED");
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action))   //如果是有效数据
			{
				//处理发送过来的有效数据
				try {
					displayData(intent.getExtras().getString(BluetoothLeService.EXTRA_DATA));
					System.out.println("BroadcastReceiver onData:"+ intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};



	/* 更新连接状态*/
	private void updateConnectionState(String status)
	{
		Message msg = new Message();
		msg.what = 1;
		Bundle b = new Bundle();
		b.putString("connect_state", status);
		msg.setData(b);
		//将连接状态更新的UI的textview上
		myHandler.sendMessage(msg);
		System.out.println("connect_state:" + status);

	}

	/* Intent过滤器 */
	private static IntentFilter makeGattUpdateIntentFilter()
	{
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}

	/**
	* @Title: displayData 
	* @Description: 将接收到的数据在scrollview上显示
	*/
	private void displayData(String rev_string)
	{
		rev_str += rev_string;
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				rev_tv.setText(rev_str);
				rev_sv.scrollTo(0, rev_tv.getMeasuredHeight());
				System.out.println("rev:" + rev_str);
			}
		});
		//rev_str = "";
	}

	/**
	 * @Title: displayGattServices
	 * @Description: 查找蓝牙服务，在服务里找到BT580透传特征值
	 * @param
	 * @return void
	 * @throws
	 */
	private void displayGattServices(List<BluetoothGattService> gattServices)
	{

		if (gattServices == null)
			return;
		String uuid = null;
		String unknownServiceString = "unknown_service";
		String unknownCharaString = "unknown_characteristic";

		// 服务数据,可扩展下拉列表的第一级数据
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

		// 特征数据（隶属于某一级服务下面的特征值集合）
		ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();

		// 部分层次，所有特征值集合
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices)
		{

			// 获取服务列表
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();

			// 查表，根据该uuid获取对应的服务名称。SampleGattAttributes这个表需要自定义。

			gattServiceData.add(currentServiceData);

			System.out.println("Service uuid:" + uuid);

			ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();

			// 从当前循环所指向的服务中读取特征值列表
			List<BluetoothGattCharacteristic> gattCharacteristics = gattService
					.getCharacteristics();

			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			// 对于当前循环所指向的服务中的每一个特征值
			for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)
			{
				charas.add(gattCharacteristic);
				HashMap<String, String> currentCharaData = new HashMap<String, String>();
				uuid = gattCharacteristic.getUuid().toString();

				if (gattCharacteristic.getUuid().toString()
						.equals(HEART_RATE_MEASUREMENT))
				{
					// 开启BT580透传特征值的Notify通知功能
					mBluetoothLeService.setCharacteristicNotification(
							gattCharacteristic, true);
					target_chara = gattCharacteristic;  //将BT580透传特征值赋值给target_chara
					// 往BT580适配器写数据
					// mBluetoothLeService.writeCharacteristic(gattCharacteristic);
				}
				List<BluetoothGattDescriptor> descriptors = gattCharacteristic
						.getDescriptors();
				for (BluetoothGattDescriptor descriptor : descriptors)
				{
					System.out.println("---descriptor UUID:"
							+ descriptor.getUuid());
					// 获取特征值的描述
					mBluetoothLeService.getCharacteristicDescriptor(descriptor);
					// mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,
					// true);
				}

				gattCharacteristicGroupData.add(currentCharaData);
			}
			// 按先后顺序，分层次放入特征值集合中，只有特征值
			mGattCharacteristics.add(charas);
			// 构件第二级扩展列表（服务下面的特征值）
			gattCharacteristicData.add(gattCharacteristicGroupData);

		}

	}

	/**
	 * 发送按键的响应事件，发送文本框的数据
	 */
	@Override
	public void onClick(View v)
	{
		// TODO Auto-generated method stub
		byte[] bytes = send_et.getText().toString().getBytes();
		int total_len = bytes.length;
		int cur_pos = 0;
		int i = 0;
		while(cur_pos<total_len)
		{
			int lenSub = 0;
			if(total_len-cur_pos>20)
				lenSub = 20;
			else
				lenSub = total_len - cur_pos;

			byte[] bytes_sub = new byte[lenSub];

			for(i = 0; i<lenSub; i++)
			{
				bytes_sub[i] = bytes[cur_pos + i];
			}
			cur_pos += lenSub;

			//DeviceScanActivity.writeChar6_in_bytes(bytes_sub);
			target_chara.setValue(bytes_sub);
			mBluetoothLeService.writeCharacteristic(target_chara);

			// 延时 一会
			try {
				Thread.sleep(40);
			} catch (InterruptedException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
		}

	}

}
