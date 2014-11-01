package com.piccal_picture_calculator;

// import OCR libraries
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.renderscript.Element;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.googlecode.tesseract.android.TessBaseAPI;
// end of import OCR libraries


import com.piccal_picture_calculator.R;
//import wolfram libraries
import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;
//end of import wolfram libraries


//import xml parser libraries
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
// end of import xml parser libraries


public class PicCalActivity extends Activity {
	public static final String PACKAGE_NAME = "com.piccal_picture_calculator";
	public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/PicCal_Picture_Calculator/";
	//public static final String CAMERA_PATH = Environment.getExternalStorageDirectory().toString() + "/SimpleAndroidOCR/";
	public Uri imageUri;
	public File image;

	// You should have the trained data file in assets folder
	// You can get them at:
	// http://code.google.com/p/tesseract-ocr/downloads/list
	public static final String lang = "eng";

	private static final String TAG = "PicCalOCR.java";

	protected Button _takepic;
	protected Button _getimage;
	protected Button _cleartext;
	protected Button _getresult;

	// protected ImageView _image;
	protected EditText _field1;
	protected TextView _field2;
	protected String _path;
	protected boolean _taken;
	protected Bitmap _bitmap = null;
	protected static final String PHOTO_TAKEN = "photo_taken";
	private static final int SELECT_PHOTO = 1;
	private static final int CAMERA_PHOTO = 2;

	private static String appid = "9LPUAP-AKX2VQAAPK";

	String RESULT = "";
	String VALUE = "";
	static int ROW = 2;
	static int COL = 10;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

		for (String path : paths) {
			File dir = new File(path);
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
					return;
				} else {
					Log.v(TAG, "Created directory " + path + " on sdcard");
				}
			}
		}
		// lang.traineddata file with the app (in assets folder) http://code.google.com/p/tesseract-ocr/downloads/list
		// This area needs work and optimization
		if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
			try {
				AssetManager assetManager = getAssets();
				InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
				//GZIPInputStream gin = new GZIPInputStream(in);
				OutputStream out = new FileOutputStream(DATA_PATH + "tessdata/" + lang + ".traineddata");

				// Transfer bytes from in to out
				byte[] buf = new byte[1024];
				int len;
				//while ((lenf = gin.read(buff)) > 0) {
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				//gin.close();
				out.close();
				Log.v(TAG, "Copied " + lang + " traineddata");
			} catch (IOException e) {
				Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
			}
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// _image = (ImageView) findViewById(R.id.image);
		_field1 = (EditText) findViewById(R.id.field1);
		_field2 = (TextView) findViewById(R.id.field2);

		_takepic   = (Button) findViewById(R.id.takepic);
		_getimage  = (Button) findViewById(R.id.getimage);
		_getresult = (Button) findViewById(R.id.getresult);
		_cleartext = (Button) findViewById(R.id.cleartext);

		_takepic.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view){
				// hide keyboard 
				hideKeyboard();

				_field1.setText("");
				_field2.setText("");

				Log.v(TAG, "Starting Camera app");
				startCameraActivity();
			}
		});

		_cleartext.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view){
				// hide keyboard 
				hideKeyboard();

				_field1.setText("");
				_field2.setText("");
			}
		});

		_getimage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {		
				// hide keyboard 
				hideKeyboard();

				Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
				photoPickerIntent.setType("image/*");
				startActivityForResult(photoPickerIntent, SELECT_PHOTO);
				_field1.setText("");
				_field2.setText("");
			}
		});

		_getresult.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view){
				// hide keyboard 
				hideKeyboard();

				// display processing text while retrieving result
				_field2.setText("Processing...");

				// get the input date from field2
				final String in = _field1.getText().toString();
				class MyThread extends Thread {
					@Override
					public void run() {
						try {
							RESULT = getResultFromWolfram(in);
						} catch (Exception e) {
							e.printStackTrace();
						}       
						// wrap the update to UI fields to a runOnUiThread method
						runOnUiThread(new Runnable() {
							@Override
							public void run() { _field2.setText(RESULT); }
						});
					}
				}
				MyThread mThread = new MyThread();	
				mThread.start();
			}
		});
		_path = DATA_PATH + "/ocr.jpg";
	}

	//	public class ButtonClickHandler implements View.OnClickListener {
	//		public void onClick(View view) {
	//			Log.v(TAG, "Starting Camera app");
	//			startCameraActivity();
	//		}
	//	}

	protected void startCameraActivity() {
		File file = new File(_path);
		Uri outputFileUri = Uri.fromFile(file);
		final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
		startActivityForResult(intent, CAMERA_PHOTO);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "resultCode: " + resultCode);
		_bitmap = null;
		try{
			if(resultCode == RESULT_OK){
				switch(requestCode) { 
				case CAMERA_PHOTO:

					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inSampleSize = 4;

					_bitmap = BitmapFactory.decodeFile(_path, options);
					onPhotoTaken();

					break;
				case SELECT_PHOTO:
					imageUri = data.getData();
					_path = imageUri.toString();
					image = new File(_path);

					InputStream imageStream = getContentResolver().openInputStream(imageUri);
					_bitmap = BitmapFactory.decodeStream(imageStream);
					onPhotoTaken();


				}
			}
		} catch (FileNotFoundException e){
			e.printStackTrace();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(PicCalActivity.PHOTO_TAKEN, _taken);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.i(TAG, "onRestoreInstanceState()");
		if (savedInstanceState.getBoolean(PicCalActivity.PHOTO_TAKEN)) {
			try {
				onPhotoTaken();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	protected void onPhotoTaken() throws FileNotFoundException{
		_taken = true;

		try {
			ExifInterface exif = new ExifInterface(_path);
			int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			Log.v(TAG, "Orient: " + exifOrientation);

			int rotate = 0;

			switch (exifOrientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				rotate = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				rotate = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				rotate = 270;
				break;
			}

			Log.v(TAG, "Rotation: " + rotate);

			if (rotate != 0) {
				// Getting width & height of the given image.
				int w = _bitmap.getWidth();
				int h = _bitmap.getHeight();
				// Setting pre rotate
				Matrix mtx = new Matrix();
				mtx.preRotate(rotate);
				// Rotating Bitmap
				_bitmap = Bitmap.createBitmap(_bitmap, 0, 0, w, h, mtx, false);
			}
			// Convert to ARGB_8888, required by tess
			_bitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);

		} catch (IOException e) {
			Log.e(TAG, "Couldn't correct orientation: " + e.toString());
		}

		// _image.setImageBitmap( bitmap );
		Log.v(TAG, "Before baseApi");
		TessBaseAPI baseApi = new TessBaseAPI();
		baseApi.setDebug(true);
		baseApi.init(DATA_PATH, lang);
		baseApi.setImage(_bitmap);
		//baseApi.setImage(image);
		String recognizedText = baseApi.getUTF8Text();
		baseApi.end();

		Log.v(TAG, "OCRED TEXT: " + recognizedText);

		recognizedText = recognizedText.trim();
		if ( recognizedText.length() != 0 ) {
			//_field1.setText(_field1.getText().toString().length() == 0 ? recognizedText : _field1.getText() + " " + recognizedText);

			// replace white space, but not new line 
			_field1.setText(recognizedText.replaceAll("((?!\n+)\\s+)",""));

			// place cursor at the end of the text in field1 for editing
			_field1.setSelection(_field1.getText().toString().length()); 


			// for debugging purpose code 145
			String output = _field1.getText().toString();

			int outputLen = output.length();
			System.out.println("MTY string is " + recognizedText);
			System.out.println("");
			for(int i = 0; i < outputLen; i++){
				System.out.println(i + "th character is: " + output.charAt(i));
			}
			// end of debugging code 145
		}
	}

	// takes in string and return result as string
	public static String getResultFromWolfram(String input) throws IOException{
		System.out.println("MTY start method getResultFromWolfram");
		System.out.println("MTY input is " + input);

		String returnStr = "";

		int podTitleCounter = 0;
		int podValueCounter = 0;
		String currentTitle = "";
		String currentValue = "";

		WAEngine engine = new WAEngine();

		// These properties will be set in all the WAQuery objects created from this WAEngine.
		engine.setAppID(appid);
		engine.addFormat("plaintext");
		engine.addFormat("image");

		WAQuery query = engine.createQuery(); // Create the query.
		query.setInput(input); // Set properties of the query.

		try {
			// For educational purposes, print out the URL we are about to send:
			System.out.println("Query URL: " + engine.toURL(query) + "\n");

			// This sends the URL to the WolframAlpha server, gets the XML result
			// and parses it into an object hierarchy held by the WAQueryResult object.
			WAQueryResult queryResult = engine.performQuery(query);

			if (queryResult.isError()) {
				System.out.println("Query error");
				System.out.println("  error code: " + queryResult.getErrorCode());
				System.out.println("  error message: " + queryResult.getErrorMessage());
			} 
			else if (!queryResult.isSuccess()) {
				System.out.println("Query was not understood; no results available.");
			} 
			else {
				// Got a result and print them accordingly
				System.out.println("--------------------- parse result start --------------------- ");

				int podLength = queryResult.getPods().length;

           		// write podXML to a file temp.xml for later parsing process
            	String podXML = queryResult.getXML();
            	System.out.println("start XML");
            	System.out.println(podXML);
            	System.out.println("end XML");
            	creatTempXML(podXML);
/*            	
            	// creating xml parser object
            	XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
            	XmlPullParser myParser = xmlFactoryObject.newPullParser();
            	
      	
            	//FileInputStream is = openFileInput(Environment.getExternalStorageDirectory().toString() + "/PicCal_Picture_Calculator/temp.xml");
            	FileInputStream is = startfile();
            	myParser.setInput(is, null);
            	String temperature = "";
            	
            	int event = myParser.getEventType();
            	while (event != XmlPullParser.END_DOCUMENT) 
            	{
            	   String name=myParser.getName();
            	   switch (event){
            	      case XmlPullParser.START_TAG:
            	      break;
            	      case XmlPullParser.END_TAG:
            	      if(name.equals("queryresult")){
            	    	  temperature = myParser.getAttributeValue(null,"host");
            	      }
            	      break;
            	   }		 
            	   event = myParser.next(); 					
            	}
            	System.out.println("start host");
            	System.out.println(temperature);
            	System.out.println("end host");
            	
            	// read and parse xml begin
            	try {
            		File fXmlFile = new File("resultxml.xml");
            		//File fXmlFile = new File(podXML);
            		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            		Document doc = dBuilder.parse(fXmlFile);
            	 
            		//optional, but recommended
            		//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            		doc.getDocumentElement().normalize();
            	 
            		System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
            	 
            		NodeList nList = doc.getElementsByTagName("pod");
            	 
            		System.out.println("----------------------------");
            	 
            		for (int temp = 0; temp < nList.getLength(); temp++) {
            	 
            			Node nNode = nList.item(temp);
            	 
            			System.out.println("\nCurrent Element :" + nNode.getNodeName());
            	 
            			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            	 
            				Element eElement = (Element) nNode;
            				System.out.println("title : " + ((org.w3c.dom.Element) eElement).getAttribute("title"));
            			}
            		}
            	} catch (Exception e) {
            		e.printStackTrace();
            	}
            	// end read and parse xml
*/

				String[] podTitleArray = new String[podLength];
				String[] podValueArray = new String[podLength];

				for (WAPod pod : queryResult.getPods()) {
					if (!pod.isError()) {
						//System.out.println(pod.getTitle()); // testing code

						currentTitle = (pod.getTitle()).toString();
						System.out.println("currentTitle " + currentTitle);

						// adding currentTitle to podTitleArray array
						podTitleArray[podTitleCounter] = currentTitle;
						podTitleCounter += 1;

						for (WASubpod subpod : pod.getSubpods()) {
							for (Object element : subpod.getContents()) {
								if (element instanceof WAPlainText) {

									currentValue = (((WAPlainText) element).getText()).toString();
									System.out.println("currentValue " + currentValue);
									System.out.println("---------------------");

									// adding currentValue to podValueArray array
									podValueArray[podValueCounter] = currentValue;
								}
							}
						}
						podValueCounter += 1;
					}
				} // We ignored many other types of WolframAlpha output, such as warnings, assumptions, etc.
				// These can be obtained by methods of WAQueryResult or objects deeper in the hierarchy.
				System.out.println("--------------------- parse result end --------------------- ");

				// adding podTitleArray and podValueArray values to a single string and return it
				for(int i = 0; i < podLength; i++){
					String title = podTitleArray[i].toString();
					String value = podValueArray[i].toString();
					returnStr = returnStr.concat(title + ": \n      " + value + "\n\n");
				}
			}
		} catch (WAException e) {
			e.printStackTrace();
		} 
		return returnStr;
	}

	// hide keyboard after user lick button 
	public void hideKeyboard() {
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}

	// wrting an input string to a file 
	public static void creatTempXML(String input) {
		try {
			File tempXML = new File(DATA_PATH + "temp.xml");
			FileOutputStream is = new FileOutputStream(tempXML);
			OutputStreamWriter osw = new OutputStreamWriter(is);    
			Writer w = new BufferedWriter(osw);
			w.write(input);
			w.close();
		} catch (IOException e) {
			System.err.println("Problem writing to the file temp.xml");
		}
	}
/*
	public Document getDomElement(String xml){
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
 
            DocumentBuilder db = dbf.newDocumentBuilder();
 
            InputSource is = new InputSource();
                is.setCharacterStream(new StringReader(xml));
                doc = db.parse(is); 
 
            } catch (ParserConfigurationException e) {
                Log.e("Error: ", e.getMessage());
                return null;
            } catch (SAXException e) {
                Log.e("Error: ", e.getMessage());
                return null;
            } catch (IOException e) {
                Log.e("Error: ", e.getMessage());
                return null;
            }
                // return DOM
            return doc;
    }
	public String getValue(Element item, String str) {      
	    NodeList n = ((Document) item).getElementsByTagName(str);        
	    return this.getElementValue(n.item(0));
	}
	 
	public final String getElementValue( Node elem ) {
         Node child;
         if( elem != null){
             if (elem.hasChildNodes()){
                 for( child = elem.getFirstChild(); child != null; child = child.getNextSibling() ){
                     if( child.getNodeType() == Node.TEXT_NODE  ){
                         return child.getNodeValue();
                     }
                 }
             }
         }
         return "";
	} 
*/
}
