
package com.kaidongyuan.qh_orders_android.track;
// default package


import java.util.Date;

/**
 *客户地址信息
 */
public class Location implements java.io.Serializable {
	public String id;
	public String userIdx;
	public Double CORDINATEX;
	public Double CORDINATEY;
	public String ADDRESS;
	public Date CREATETIME;
	public Double lat;
	public Double lon;

	@Override
	public String toString() {
		return "lat:"+lat+"\t,lon"+lon;
	}
}