package com.rentalapp.base;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.rentalapp.bean.ColHouse;
import com.rentalapp.bean.Cost;
import com.rentalapp.bean.House;
import com.rentalapp.bean.Maintenance;
import com.rentalapp.Rent.Renting;
import com.rentalapp.bean.User;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    /**
     * 声明一个AndroidSDK自带的数据库变量db
     */
    private SQLiteDatabase db;

    /**
     * 写一个这个类的构造函数，参数为上下文context，所谓上下文就是这个类所在包的路径
     */
    public DatabaseHelper(Context context) {
        super(context, "rentalapp", null, 2);
        db = getReadableDatabase();
    }

    /**
     * 重写两个必须要重写的方法，因为class DBOpenHelper extends SQLiteOpenHelper
     * 而这两个方法是 abstract 类 SQLiteOpenHelper 中声明的 abstract 方法
     * 所以必须在子类 DBOpenHelper 中重写 abstract 方法
     *
     * @param db
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        //用户表
        db.execSQL("CREATE TABLE IF NOT EXISTS users(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "email TEXT," +
                "password TEXT," +
                "truename TEXT," +
                "phone TEXT," +
                "role TEXT)");
        //房源表
        db.execSQL("CREATE TABLE IF NOT EXISTS houses(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uid INTEGER," +
                "title TEXT," +
                "price INTEGER," +
                "area INTEGER," +
                "address TEXT," +
                "powerrate TEXT," +
                "imgpath TEXT," +
                "housetype TEXT," +
                "pdfpath TEXT," +
                "remark TEXT," +
                "uname TEXT," +
                "uphone TEXT," +
                "umail TEXT," +
                "lng TEXT," +
                "lat TEXT," +
                "status TEXT)");
        //租房表
        db.execSQL("CREATE TABLE IF NOT EXISTS rentings(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uid INTEGER," +
                "hid INTEGER," +
                "signature TEXT," +
                "contract TEXT," +
                "rentaltime TEXT," +
                "addtime TEXT," +
                "status TEXT)");
        //维护表
        db.execSQL("CREATE TABLE IF NOT EXISTS maintenances(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uid INTEGER," +
                "lid INTEGER," +
                "hid INTEGER," +
                "content TEXT," +
                "applytime TEXT," +
                "status TEXT)");
        //缴费表
        db.execSQL("CREATE TABLE IF NOT EXISTS costs(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uid INTEGER," +
                "hid INTEGER," +
                "category TEXT," +
                "money TEXT," +
                "remark TEXT," +
                "addtime TEXT)");
        //收藏表
        db.execSQL("CREATE TABLE IF NOT EXISTS cols(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uid INTEGER," +
                "hid INTEGER," +
                "title TEXT," +
                "address TEXT)");

        //初始化
        initDataBase(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE houses ADD COLUMN rentaltime TEXT");
            db.execSQL("ALTER TABLE houses ADD COLUMN tenantUid TEXT");
            db.execSQL("ALTER TABLE houses ADD COLUMN contract TEXT");
            db.execSQL("ALTER TABLE houses ADD COLUMN signature TEXT");
        }
    }


    //初始化数据库
    private void initDataBase (SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put("email","admin@qq.com");
        values.put("password","admin");
        values.put("role","admin");
        values.put("truename","admin");
        values.put("phone","");
        db.insert("users", null, values);
    }

    //获取用户信息
    @SuppressLint("Range")
    public User getUserInfo(String email) {
        String sql = "select * from users where email=?";
        Cursor cursor = db.rawQuery(sql, new String[]{email});
        User data = null;
        while (cursor.moveToNext()) {
            data = new User();
            data.setId(cursor.getInt(cursor.getColumnIndex("_id")));
            data.setEmail(cursor.getString(cursor.getColumnIndex("email")));
            data.setPassword(cursor.getString(cursor.getColumnIndex("password")));
            data.setTruename(cursor.getString(cursor.getColumnIndex("truename")));
            data.setPhone(cursor.getString(cursor.getColumnIndex("phone")));
            data.setRole(cursor.getString(cursor.getColumnIndex("role")));
        }
        return data;
    }

    //添加用户
    public boolean addUser(String email,String password,String role){
        ContentValues values = new ContentValues();
        values.put("email",email);
        values.put("password", password);
        values.put("role",role);
        return db.insert("users","_id",values)>0;
    }

    //获取待审核的房源列表
    @SuppressLint("Range")
    public List<House> getHouseForCheck(){
        List<House> list = new ArrayList<>();
        Cursor cursor = db.query("houses",null,"status='nocheck'",null,null,null,"_id desc");
        while (cursor.moveToNext()) {
            House data = new House();
            data.setId(cursor.getInt(cursor.getColumnIndex("_id")));
            data.setUid(cursor.getInt(cursor.getColumnIndex("uid")));
            data.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            data.setPrice(cursor.getInt(cursor.getColumnIndex("price")));
            data.setArea(cursor.getInt(cursor.getColumnIndex("area")));
            data.setAddress(cursor.getString(cursor.getColumnIndex("address")));
            data.setHousetype(cursor.getString(cursor.getColumnIndex("housetype")));
            data.setPowerrate(cursor.getString(cursor.getColumnIndex("powerrate")));
            data.setRemark(cursor.getString(cursor.getColumnIndex("remark")));
            data.setImgpath(cursor.getString(cursor.getColumnIndex("imgpath")));
            data.setStatus(cursor.getString(cursor.getColumnIndex("status")));
            data.setPdfpath(cursor.getString(cursor.getColumnIndex("pdfpath")));
            data.setUname(cursor.getString(cursor.getColumnIndex("uname")));
            data.setUphone(cursor.getString(cursor.getColumnIndex("uphone")));
            data.setUmail(cursor.getString(cursor.getColumnIndex("umail")));
            data.setLng(cursor.getString(cursor.getColumnIndex("lng")));
            data.setLat(cursor.getString(cursor.getColumnIndex("lat")));
            list.add(data);
        }
        return list;
    }

    //获取房源详情
    @SuppressLint("Range")
    public House getHouse(int id) {
        House data = null;
        Cursor cursor = db.query("houses", null, "_id = " + id, null, null, null, null);
        if (cursor.moveToNext()) {
            data = new House();
            data.setId(cursor.getInt(cursor.getColumnIndex("_id")));
            data.setUid(cursor.getInt(cursor.getColumnIndex("uid")));
            data.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            data.setPrice(cursor.getInt(cursor.getColumnIndex("price")));
            data.setArea(cursor.getInt(cursor.getColumnIndex("area")));
            data.setAddress(cursor.getString(cursor.getColumnIndex("address")));
            data.setHousetype(cursor.getString(cursor.getColumnIndex("housetype")));
            data.setPowerrate(cursor.getString(cursor.getColumnIndex("powerrate")));
            data.setRemark(cursor.getString(cursor.getColumnIndex("remark")));
            data.setImgpath(cursor.getString(cursor.getColumnIndex("imgpath")));
            data.setStatus(cursor.getString(cursor.getColumnIndex("status")));
            data.setPdfpath(cursor.getString(cursor.getColumnIndex("pdfpath")));
            data.setUname(cursor.getString(cursor.getColumnIndex("uname")));
            data.setUphone(cursor.getString(cursor.getColumnIndex("uphone")));
            data.setUmail(cursor.getString(cursor.getColumnIndex("umail")));
            data.setLng(cursor.getString(cursor.getColumnIndex("lng")));
            data.setLat(cursor.getString(cursor.getColumnIndex("lat")));
        }
        return data;
    }

    //审核房源
    public boolean checkHouse(int id, String status){
        ContentValues values = new ContentValues();
        values.put("status", status);
        return db.update("houses",values,"_id=?",new String[]{""+id}) > 0;
    }

    //删除房源
    public boolean delHouse(int id){
        return db.delete("houses","_id=?",new String[]{""+id})>0;
    }

    //获取房东自己的房源
    @SuppressLint("Range")
    public List<House> getHouses(String uid){
        List<House> list = new ArrayList<>();
        Cursor cursor = db.query("houses",null,"uid="+uid,null,null,null,"_id desc");
        while (cursor.moveToNext()) {
            House data = new House();
            data.setId(cursor.getInt(cursor.getColumnIndex("_id")));
            data.setUid(cursor.getInt(cursor.getColumnIndex("uid")));
            data.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            data.setPrice(cursor.getInt(cursor.getColumnIndex("price")));
            data.setArea(cursor.getInt(cursor.getColumnIndex("area")));
            data.setAddress(cursor.getString(cursor.getColumnIndex("address")));
            data.setHousetype(cursor.getString(cursor.getColumnIndex("housetype")));
            data.setPowerrate(cursor.getString(cursor.getColumnIndex("powerrate")));
            data.setRemark(cursor.getString(cursor.getColumnIndex("remark")));
            data.setImgpath(cursor.getString(cursor.getColumnIndex("imgpath")));
            data.setStatus(cursor.getString(cursor.getColumnIndex("status")));
            data.setPdfpath(cursor.getString(cursor.getColumnIndex("pdfpath")));
            data.setUname(cursor.getString(cursor.getColumnIndex("uname")));
            data.setUphone(cursor.getString(cursor.getColumnIndex("uphone")));
            data.setUmail(cursor.getString(cursor.getColumnIndex("umail")));
            data.setLng(cursor.getString(cursor.getColumnIndex("lng")));
            data.setLat(cursor.getString(cursor.getColumnIndex("lat")));
            list.add(data);
        }
        return list;
    }

    //获取自己租的房源
    @SuppressLint("Range")
    public List<House> getMyHouses(String uid){
        List<House> list = new ArrayList<>();
        Cursor cursor1 = db.query("rentings",null,"uid=?",new String[]{uid},null,null,"_id desc");
        while (cursor1.moveToNext()) {
            int house_id = cursor1.getInt(cursor1.getColumnIndex("hid"));  // ✅ 修复：用 hid 而不是 _id
            Cursor cursor = db.query("houses", null, "_id=?", new String[]{String.valueOf(house_id)}, null, null, null);
            while (cursor.moveToNext()) {
                House data = new House();
                data.setId(cursor.getInt(cursor.getColumnIndex("_id")));
                data.setUid(cursor.getInt(cursor.getColumnIndex("uid")));
                data.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                data.setPrice(cursor.getInt(cursor.getColumnIndex("price")));
                data.setArea(cursor.getInt(cursor.getColumnIndex("area")));
                data.setAddress(cursor.getString(cursor.getColumnIndex("address")));
                data.setHousetype(cursor.getString(cursor.getColumnIndex("housetype")));
                data.setPowerrate(cursor.getString(cursor.getColumnIndex("powerrate")));
                data.setRemark(cursor.getString(cursor.getColumnIndex("remark")));
                data.setImgpath(cursor.getString(cursor.getColumnIndex("imgpath")));
                data.setStatus(cursor.getString(cursor.getColumnIndex("status")));
                data.setPdfpath(cursor.getString(cursor.getColumnIndex("pdfpath")));
                data.setUname(cursor.getString(cursor.getColumnIndex("uname")));
                data.setUphone(cursor.getString(cursor.getColumnIndex("uphone")));
                data.setUmail(cursor.getString(cursor.getColumnIndex("umail")));
                data.setLng(cursor.getString(cursor.getColumnIndex("lng")));
                data.setLat(cursor.getString(cursor.getColumnIndex("lat")));
                list.add(data);
            }
            cursor.close(); // ✅ 避免游标泄露
        }
        cursor1.close(); // ✅ 添加关闭
        return list;
    }


    //获取所有审核通过的房源
    @SuppressLint("Range")
    public List<House> getAllHouses(){
        List<House> list = new ArrayList<>();
        Cursor cursor = db.query("houses",null,"status='agree'",null,null,null,"_id desc");
        while (cursor.moveToNext()) {
            House data = new House();
            data.setId(cursor.getInt(cursor.getColumnIndex("_id")));
            data.setUid(cursor.getInt(cursor.getColumnIndex("uid")));
            data.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            data.setPrice(cursor.getInt(cursor.getColumnIndex("price")));
            data.setArea(cursor.getInt(cursor.getColumnIndex("area")));
            data.setAddress(cursor.getString(cursor.getColumnIndex("address")));
            data.setHousetype(cursor.getString(cursor.getColumnIndex("housetype")));
            data.setPowerrate(cursor.getString(cursor.getColumnIndex("powerrate")));
            data.setRemark(cursor.getString(cursor.getColumnIndex("remark")));
            data.setImgpath(cursor.getString(cursor.getColumnIndex("imgpath")));
            data.setStatus(cursor.getString(cursor.getColumnIndex("status")));
            data.setPdfpath(cursor.getString(cursor.getColumnIndex("pdfpath")));
            data.setUname(cursor.getString(cursor.getColumnIndex("uname")));
            data.setUphone(cursor.getString(cursor.getColumnIndex("uphone")));
            data.setUmail(cursor.getString(cursor.getColumnIndex("umail")));
            data.setLng(cursor.getString(cursor.getColumnIndex("lng")));
            data.setLat(cursor.getString(cursor.getColumnIndex("lat")));
            list.add(data);
        }
        return list;
    }

    //添加房源
    public boolean addHouse(House house){
        ContentValues values = new ContentValues();
        values.put("uid",house.getUid());
        values.put("title",house.getTitle());
        values.put("price", house.getPrice());
        values.put("area",house.getArea());
        values.put("address",house.getAddress());
        values.put("powerrate",house.getPowerrate());
        values.put("imgpath",house.getImgpath());
        values.put("housetype",house.getHousetype());
        values.put("pdfpath",house.getPdfpath());
        values.put("remark",house.getRemark());
        values.put("uname",house.getUname());
        values.put("uphone",house.getUphone());
        values.put("umail",house.getUmail());
        values.put("lng",house.getLng());
        values.put("lat",house.getLat());
        values.put("status",house.getStatus());
        return db.insert("houses","_id",values)>0;
    }

    //修改房源
    public boolean updateHouse(House house){
        ContentValues values = new ContentValues();
        values.put("title",house.getTitle());
        values.put("price", house.getPrice());
        values.put("area",house.getArea());
        values.put("address",house.getAddress());
        values.put("powerrate",house.getPowerrate());
        values.put("imgpath",house.getImgpath());
        values.put("housetype",house.getHousetype());
        values.put("pdfpath",house.getPdfpath());
        values.put("remark",house.getRemark());
        values.put("lng",house.getLng());
        values.put("lat",house.getLat());
        return db.update("houses",values,"_id=?",new String[]{house.getId()+""}) > 0;
    }

    //获取最后一个房源
    @SuppressLint("Range")
    public House getLastHouse() {
        House data = new House();
        Cursor cursor = db.query("houses", null, null, null, null, null, "_id desc", "1");
        if (cursor.moveToNext()) {
            data.setId(cursor.getInt(cursor.getColumnIndex("_id")));
            data.setUid(cursor.getInt(cursor.getColumnIndex("uid")));
            data.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            data.setPrice(cursor.getInt(cursor.getColumnIndex("price")));
            data.setArea(cursor.getInt(cursor.getColumnIndex("area")));
            data.setAddress(cursor.getString(cursor.getColumnIndex("address")));
            data.setHousetype(cursor.getString(cursor.getColumnIndex("housetype")));
            data.setPowerrate(cursor.getString(cursor.getColumnIndex("powerrate")));
            data.setRemark(cursor.getString(cursor.getColumnIndex("remark")));
            data.setImgpath(cursor.getString(cursor.getColumnIndex("imgpath")));
            data.setStatus(cursor.getString(cursor.getColumnIndex("status")));
            data.setPdfpath(cursor.getString(cursor.getColumnIndex("pdfpath")));
            data.setUname(cursor.getString(cursor.getColumnIndex("uname")));
            data.setUphone(cursor.getString(cursor.getColumnIndex("uphone")));
            data.setUmail(cursor.getString(cursor.getColumnIndex("umail")));
            data.setLng(cursor.getString(cursor.getColumnIndex("lng")));
            data.setLat(cursor.getString(cursor.getColumnIndex("lat")));
        }
        return data;
    }

    //添加收藏房源
    public boolean addCol(int uid,int hid,String title,String address){
        ContentValues values = new ContentValues();
        values.put("uid",uid);
        values.put("hid",hid);
        values.put("title",title);
        values.put("address",address);
        return db.insert("cols","_id",values)>0;
    }

    //获取我收藏的房源
    @SuppressLint("Range")
    public List<ColHouse> getColHouses(String uid) {
        List<ColHouse> list = new ArrayList<>();
        Cursor cursor = db.query("cols",null,"uid="+uid,null,null,null,"_id desc");
        while (cursor.moveToNext()) {
            ColHouse data = new ColHouse();
            data.setId(cursor.getInt(cursor.getColumnIndex("_id")));
            data.setUid(cursor.getInt(cursor.getColumnIndex("uid")));
            data.setHid(cursor.getInt(cursor.getColumnIndex("hid")));
            data.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            data.setAddress(cursor.getString(cursor.getColumnIndex("address")));
            list.add(data);
        }
        return list;
    }

    //删除收藏的房源
    public boolean delColHouse(int id){
        return db.delete("cols","_id=?",new String[]{""+id})>0;
    }

    //添加维修申请
    public boolean addMaintenance(Maintenance maintenance){
        ContentValues values = new ContentValues();
        values.put("uid",maintenance.getUid());
        values.put("lid",maintenance.getLid());
        values.put("hid",maintenance.getHid());
        values.put("content",maintenance.getContent());
        values.put("applytime",maintenance.getApplytime());
        values.put("status",maintenance.getStatus());

        // 🟢【插入位置：DatabaseHelper.java -> addMaintenance() 方法最后】
        FirebaseSync.syncMaintenanceToCloud(maintenance);  // 🔄同步到云端
        return db.insert("maintenances","_id",values)>0;


    }

    //获取自己房源的维修申请
    @SuppressLint("Range")
    public List<Maintenance> getMaintenances(int lid){
        List<Maintenance> list = new ArrayList<>();
        String query = "select maintenances.*,users.truename,users.phone,houses.title,houses.address from maintenances,users,houses where maintenances.uid=users._id and maintenances.hid=houses._id and maintenances.lid="+lid+" order by maintenances.applytime desc";
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            Maintenance maintenance = new Maintenance();
            maintenance.setId(cursor.getInt(cursor.getColumnIndex("_id")));
            maintenance.setUid(cursor.getInt(cursor.getColumnIndex("uid")));
            maintenance.setLid(cursor.getInt(cursor.getColumnIndex("lid")));
            maintenance.setHid(cursor.getInt(cursor.getColumnIndex("hid")));
            maintenance.setUname(cursor.getString(cursor.getColumnIndex("truename")));
            maintenance.setUphone(cursor.getString(cursor.getColumnIndex("phone")));
            maintenance.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            maintenance.setAddress(cursor.getString(cursor.getColumnIndex("address")));
            maintenance.setContent(cursor.getString(cursor.getColumnIndex("content")));
            maintenance.setApplytime(cursor.getString(cursor.getColumnIndex("applytime")));
            maintenance.setStatus(cursor.getString(cursor.getColumnIndex("status")));
            list.add(maintenance);
        }
        return list;
    }

    //处理维修申请
    public boolean updateStatus(int id){
        ContentValues values = new ContentValues();
        values.put("status","finish");
        return db.update("maintenances",values,"_id=?",new String[]{id+""}) > 0;
    }

    //进行缴费
    public boolean addCost(Cost cost){
        ContentValues values = new ContentValues();
        values.put("uid",cost.getUid());
        values.put("hid",cost.getHid());
        values.put("category",cost.getCategory());
        values.put("money",cost.getMoney());
        values.put("addtime",cost.getAddtime());
        values.put("remark",cost.getRemark());
        return db.insert("costs","_id",values)>0;
    }

    //进行租房
    public boolean addRenting(Renting renting){
        ContentValues values = new ContentValues();
        values.put("uid",renting.getUid());
        values.put("hid",renting.getHid());
        values.put("signature",renting.getSignature());
        values.put("contract",renting.getContract());
        values.put("rentaltime",renting.getRentaltime());
        values.put("addtime",renting.getAddtime());
        values.put("status",renting.getStatus());
        return db.insert("rentings","_id",values)>0;
    }

    //用户身份变更
    public boolean updateRole(String uid,String role){
        ContentValues values = new ContentValues();
        values.put("role",role);
        return db.update("users",values,"_id=?",new String[]{uid}) > 0;
    }

    //房源状态变更
    public boolean updateHouseStatus(String id,String status){
        ContentValues values = new ContentValues();
        values.put("status",status);
        return db.update("houses",values,"_id=?",new String[]{id}) > 0;
    }

    //房源高级检索
    @SuppressLint("Range")
    public List<House> queryHouse(String title,String minprice,String maxprice,String minarea,String maxarea,String sel_bed,String sel_bath){
        List<House> list = new ArrayList<>();
        String query = "select * from houses where 1=1";
        if(!title.equals("")){
            query = query + " and title like '%"+title+"%'";
        }

        // ❌ 使用的是“开区间”（不包含边界），导致很多正好等于 min/max 的结果被排除；
        // ❌ 没有限定 status = 'agree'，可能查到未审核房源；
        /*
        if(!minprice.equals("")){
            query = query + " and price > " + minprice;
        }
        if(!maxprice.equals("")){
            query = query + " and price < " + maxprice;
        }
        */

        /*
        ❗问题在于：
        用户未输入 area，但仍然拼接了 and area > 这样的 SQL 片段（可能是空字符串），最终导致 SQL 出错或始终返回空结果；
        并且 " and status='agree'" 位置靠前，也容易掩盖问题调试；
         */

        /*
        if(!minprice.equals("")){
            query = query + " and price >= " + minprice;
        }
        if(!maxprice.equals("")){
            query = query + " and price <= " + maxprice;
        }
        // ✅ 限定只搜索已通过审核的房源
        query = query + " and status='agree'";

        if(!minarea.equals("")){
            query = query + " and area > " + minarea;
        }
        if(!maxarea.equals("")){
            query = query + " and area < " + maxarea;
        }
        */

        if (!minprice.equals("")) {
            query += " and price >= " + minprice;
        }
        if (!maxprice.equals("")) {
            query += " and price <= " + maxprice;
        }
        if (!minarea.equals("")) {
            query += " and area >= " + minarea;
        }
        if (!maxarea.equals("")) {
            query += " and area <= " + maxarea;
        }


// ✅Changed【已修改为包含 agree 与 rented】
        query += " and (status='agree' OR status='rented')";



        if(!sel_bed.equals("")){
            query = query + " and housetype like '" + sel_bed + " bedrooms%'";
        }
        if(!sel_bath.equals("")){
            query = query + " and housetype like '%" + sel_bath + " bathrooms'";
        }

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            House data = new House();
            data.setId(cursor.getInt(cursor.getColumnIndex("_id")));
            data.setUid(cursor.getInt(cursor.getColumnIndex("uid")));
            data.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            data.setPrice(cursor.getInt(cursor.getColumnIndex("price")));
            data.setArea(cursor.getInt(cursor.getColumnIndex("area")));
            data.setAddress(cursor.getString(cursor.getColumnIndex("address")));
            data.setHousetype(cursor.getString(cursor.getColumnIndex("housetype")));
            data.setPowerrate(cursor.getString(cursor.getColumnIndex("powerrate")));
            data.setRemark(cursor.getString(cursor.getColumnIndex("remark")));
            data.setImgpath(cursor.getString(cursor.getColumnIndex("imgpath")));
            data.setStatus(cursor.getString(cursor.getColumnIndex("status")));
            data.setPdfpath(cursor.getString(cursor.getColumnIndex("pdfpath")));
            data.setUname(cursor.getString(cursor.getColumnIndex("uname")));
            data.setUphone(cursor.getString(cursor.getColumnIndex("uphone")));
            data.setUmail(cursor.getString(cursor.getColumnIndex("umail")));
            data.setLng(cursor.getString(cursor.getColumnIndex("lng")));
            data.setLat(cursor.getString(cursor.getColumnIndex("lat")));
            list.add(data);
        }
        return list;
    }

    //更新手机号
    public boolean updatePhone(String uid,String phone){
        ContentValues values = new ContentValues();
        values.put("phone",phone);
        return db.update("users",values,"_id=?",new String[]{uid}) > 0;
    }

    //更新邮箱
    public boolean updateEmail(String uid,String mail){
        ContentValues values = new ContentValues();
        values.put("email",mail);
        return db.update("users",values,"_id=?",new String[]{uid}) > 0;
    }

    //更新姓名
    public boolean updateName(String uid,String truename){
        ContentValues values = new ContentValues();
        values.put("truename",truename);
        return db.update("users",values,"_id=?",new String[]{uid}) > 0;
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        Cursor cursor = db.query("users", null, null, null, null, null, "_id asc");
        while (cursor.moveToNext()) {
            User user = new User();

            int idIndex = cursor.getColumnIndex("_id");
            int emailIndex = cursor.getColumnIndex("email");
            int passwordIndex = cursor.getColumnIndex("password");
            int roleIndex = cursor.getColumnIndex("role");
            int truenameIndex = cursor.getColumnIndex("truename");
            int phoneIndex = cursor.getColumnIndex("phone");

            if (idIndex >= 0) user.setId(cursor.getInt(idIndex));
            if (emailIndex >= 0) user.setEmail(cursor.getString(emailIndex));
            if (passwordIndex >= 0) user.setPassword(cursor.getString(passwordIndex));
            if (roleIndex >= 0) user.setRole(cursor.getString(roleIndex));
            if (truenameIndex >= 0) user.setTruename(cursor.getString(truenameIndex));
            if (phoneIndex >= 0) user.setPhone(cursor.getString(phoneIndex));

            list.add(user);
        }
        cursor.close();
        return list;
    }

    public List<House> getAllHousesIncludeUnchecked() {
        List<House> list = new ArrayList<>();
        Cursor cursor = db.query("houses", null,
                "status = ? OR status = ?", new String[]{"agree", "rented"},
                null, null, "_id desc");

        while (cursor.moveToNext()) {
            House house = new House();

            int id = cursor.getColumnIndex("_id");
            if (id >= 0) house.setId(cursor.getInt(id));

            int uid = cursor.getColumnIndex("uid");
            if (uid >= 0) house.setUid(cursor.getInt(uid));

            int title = cursor.getColumnIndex("title");
            if (title >= 0) house.setTitle(cursor.getString(title));

            int price = cursor.getColumnIndex("price");
            if (price >= 0) house.setPrice(cursor.getInt(price));

            int area = cursor.getColumnIndex("area");
            if (area >= 0) house.setArea(cursor.getInt(area));

            int address = cursor.getColumnIndex("address");
            if (address >= 0) house.setAddress(cursor.getString(address));

            int powerrate = cursor.getColumnIndex("powerrate");
            if (powerrate >= 0) house.setPowerrate(cursor.getString(powerrate));

            int img = cursor.getColumnIndex("imgpath");
            if (img >= 0) house.setImgpath(cursor.getString(img));

            int type = cursor.getColumnIndex("housetype");
            if (type >= 0) house.setHousetype(cursor.getString(type));

            int pdf = cursor.getColumnIndex("pdfpath");
            if (pdf >= 0) house.setPdfpath(cursor.getString(pdf));

            int remark = cursor.getColumnIndex("remark");
            if (remark >= 0) house.setRemark(cursor.getString(remark));

            int uname = cursor.getColumnIndex("uname");
            if (uname >= 0) house.setUname(cursor.getString(uname));

            int uphone = cursor.getColumnIndex("uphone");
            if (uphone >= 0) house.setUphone(cursor.getString(uphone));

            int umail = cursor.getColumnIndex("umail");
            if (umail >= 0) house.setUmail(cursor.getString(umail));

            int lng = cursor.getColumnIndex("lng");
            if (lng >= 0) house.setLng(cursor.getString(lng));

            int lat = cursor.getColumnIndex("lat");
            if (lat >= 0) house.setLat(cursor.getString(lat));

            int status = cursor.getColumnIndex("status");
            if (status >= 0) house.setStatus(cursor.getString(status));
            int tenantUid = cursor.getColumnIndex("tenantUid");
            if (tenantUid >= 0) house.setTenantUid(cursor.getString(tenantUid));

            list.add(house);
        }

        cursor.close();
        return list;
    }

    // 🟡 添加方法：清空所有房源数据（admin专用）
    public boolean deleteAllHouses() {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete("house", null, null);
        return rows >= 0;
    }

    public boolean checkRentingExists(int uid, int hid) {
        Cursor cursor = db.rawQuery("SELECT * FROM rentings WHERE uid=? AND hid=?", new String[]{uid + "", hid + ""});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    // 🟣 DatabaseHelper.java（添加方法）
    public boolean updateHouseRentalInfo(House house) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", house.getStatus());
        values.put("tenantUid", house.getTenantUid());
        values.put("rentaltime", house.getRentaltime());
        values.put("contract", house.getContract());
        values.put("signature", house.getSignature());

        int rows = db.update("houses", values, "_id=?", new String[]{String.valueOf(house.getId())});
        return rows > 0;
    }

    // ✅插入位置示例（可放在 getMyHouses 之后）
    @SuppressLint("Range")
    public List<House> getAllRentedHouses() {
        List<House> list = new ArrayList<>();
        Cursor cursor = db.query("houses", null, "status='rented'", null, null, null, "_id desc");
        while (cursor.moveToNext()) {
            House data = new House();
            data.setId(cursor.getInt(cursor.getColumnIndex("_id")));
            data.setUid(cursor.getInt(cursor.getColumnIndex("uid")));
            data.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            data.setPrice(cursor.getInt(cursor.getColumnIndex("price")));
            data.setArea(cursor.getInt(cursor.getColumnIndex("area")));
            data.setAddress(cursor.getString(cursor.getColumnIndex("address")));
            data.setHousetype(cursor.getString(cursor.getColumnIndex("housetype")));
            data.setPowerrate(cursor.getString(cursor.getColumnIndex("powerrate")));
            data.setRemark(cursor.getString(cursor.getColumnIndex("remark")));
            data.setImgpath(cursor.getString(cursor.getColumnIndex("imgpath")));
            data.setStatus(cursor.getString(cursor.getColumnIndex("status")));
            data.setPdfpath(cursor.getString(cursor.getColumnIndex("pdfpath")));
            data.setUname(cursor.getString(cursor.getColumnIndex("uname")));
            data.setUphone(cursor.getString(cursor.getColumnIndex("uphone")));
            data.setUmail(cursor.getString(cursor.getColumnIndex("umail")));
            data.setLng(cursor.getString(cursor.getColumnIndex("lng")));
            data.setLat(cursor.getString(cursor.getColumnIndex("lat")));
            list.add(data);
        }
        cursor.close();
        return list;
    }

    // ✅ 完整删除一个房源及其相关数据
    public boolean deleteHouseAndRelated(int houseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("rentings", "hid=?", new String[]{houseId + ""});
        db.delete("maintenances", "hid=?", new String[]{houseId + ""});
        db.delete("costs", "hid=?", new String[]{houseId + ""});
        db.delete("cols", "hid=?", new String[]{houseId + ""});
        int rows = db.delete("houses", "_id=?", new String[]{houseId + ""});
        return rows > 0;
    }

    // 🟣 DatabaseHelper.java - 插入于房源相关操作附近或文件末尾
    public void deleteHouse(int houseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int deletedRows = db.delete("houses", "id = ?", new String[]{String.valueOf(houseId)});
        Log.d("DB_DELETE", "🗑 Deleted house ID = " + houseId + ", rows affected = " + deletedRows);
        db.close();
    }


}