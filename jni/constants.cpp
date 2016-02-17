/* C++ */

#include <string.h>
#include <jni.h>
#include <android/log.h>

#define  LOG_TAG    "constants.cpp"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

char data[] = 
{
	//Anti Piracy inclusions:
    // Package names                                       // App names
    "com.dimonvideo.luckypatcher",                         // Lucky patcher
    "com.chelpus.lackypatch",                              // Another lucky patcher
    "com.forpda.lp",                                       // And another one
    "com.blackmartalpha",                                  // Black Mart alpha
    "org.blackmart.market",                                // Black Mart
    "com.android.vending.billing.InAppBillingService.LUCK",// Lucky patcher 5.6.8
    "cc.madkite.freedom",                                  // Freedom
    "com.allinone.free",                                   // All-in-one Downloader
    "com.repodroid.app",                                   // Get Apk Market
    "org.creeplays.hack",                                  // CreeHack
    "com.baseappfull.fwd",                                 // Game Hacker
    "com.zmapp",                                           // Z market
    "com.dv.marketmod.installer",                          // Hacked play store that gives refunds without uninstalling the apk
    "org.mobilism.android",                                // Mobilism market

    //Anti Malware inclustions:
    "com.android.wp.net.log",                                // Ghost Push Trojan: Timeservice
    "com.android.camera.update"                              // Ghost Push Trojan infected
};

extern "C" 
{
    JNIEXPORT jobjectArray JNICALL 
		org_antipiracy_support_AntiPiracyInstallreceiver_getConstants(JNIEnv *env, jobject jobj) 
	{

		jobjectArray ret;
		int i;

		ret= (jobjectArray)env->NewObjectArray(5,env->FindClass("java/lang/String"),env->NewStringUTF(""));

		for (i=0;i<5;i++) env->SetObjectArrayElement(ret,i,env->NewStringUTF(data[i]));

		return(ret);
	}
}
