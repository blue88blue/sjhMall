package com.atsjh.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.atsjh.gulimall.product.service.CategoryBrandRelationService;
import com.atsjh.gulimall.product.vo.Catelog2Vo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.chrono.IsoChronology;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.Query;

import com.atsjh.gulimall.product.dao.CategoryDao;
import com.atsjh.gulimall.product.entity.CategoryEntity;
import com.atsjh.gulimall.product.service.CategoryService;
import org.springframework.validation.annotation.Validated;

import javax.jws.Oneway;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //查出所有分类
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        //组装成父子的树型结构
        //找到所有一级分类
        List<CategoryEntity> level1Menus = categoryEntities.stream().filter((a) -> a.getParentCid() == 0)
                .map((a) -> {
                    a.setChildren(getChildren(a, categoryEntities));
                    return a;
                })
                .sorted((a, b)->{
                    return a.getSort() - b.getSort();
                })
                .collect(Collectors.toList());

        //找到每个分类的子分类


        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1.检查当前删除的菜单， 是否被别的地方引用
        baseMapper.deleteBatchIds(asList);
    }



    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all){

        List<CategoryEntity> children = all.stream().filter((a) -> root.getCatId().equals(a.getParentCid()))
                .map((a)->{
                    a.setChildren(getChildren(a, all));
                    return a;
                })
                .sorted((a, b)->{
                    int x1 = a.getSort() == null ? 0 : a.getSort();
                    int x2 = b.getSort() == null ? 0 : b.getSort();
                    return x1 - x2;
                })
                .collect(Collectors.toList());
        return children;
    }

    @Override
    public Long[] getLogPath(Long catId) {
        ArrayList<Long> list = new ArrayList<>();
        getParent(catId, list);

        Collections.reverse(list);
        return list.toArray(new Long[list.size()]);
    }

    @CacheEvict(value = "category", allEntries = true) //失效模式， 数据库修改，删除相关缓存
    @Override
    public void updateDetail(CategoryEntity category) {
        this.updateById(category);
        if(!StringUtils.isEmpty(category.getName())){
            categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
        }
    }

    @Cacheable(value = "category", key = "#root.methodName")
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        System.out.println("方法调用：getLevel1Categorys");
        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<CategoryEntity>().eq("parent_cid", 0);
        List<CategoryEntity> categoryEntities = this.baseMapper.selectList(wrapper);
        return categoryEntities;
    }


    private void getParent(Long catId, ArrayList<Long> list) {
        list.add(catId);
        CategoryEntity byId = this.getById(catId);
        if(byId.getParentCid() != 0){
            getParent(byId.getParentCid(), list);
        }
    }

    @Cacheable(value = "category", key = "#root.methodName")
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        return getCatalogJsonFromDB();
    }


    /**
     * 查询首页的二级与三级分类数据
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJson_my() {
        /**
         * 1.缓存穿透：查询不存在的数据，都要过一遍数据库。——空结果缓存解决
         *
         * 2.缓存雪崩：大量key同一时间过期。——设置随机过期时间解决
         *
         * 3.缓存击穿：热点数据过期,大量请求同时查数据库。——加锁，只让一个线程去查然后放入缓存。
         */
        //首先向缓存查询， 如果缓存没有再向数据库查询并放入缓存
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if(StringUtils.isEmpty(catalogJson)){
            System.out.println("缓存未命中， 将要查询数据库");
            Map<String, List<Catelog2Vo>> catalogJsonFromDB = getCatalogJsonFromDBResdisLock();
            return catalogJsonFromDB;
        }
        System.out.println("缓存命中！");
        //将json字符串转换为对象
        Map<String, List<Catelog2Vo>> stringListMap = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {});
        return stringListMap;
    }

    /**
     * 使用分布式锁
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBResdisLock() {
        //使用redis占坑
        //设置UUID是防止lock过期后删了别人的锁，删锁之前要判断是自己的锁
        String uuid = UUID.randomUUID().toString();
        //加死亡时间，防止机器断电后锁一直被占用，产生死锁； 【set与设置时间要为源自操作】
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if(lock){
            System.out.println("获取分布式锁成功...");
            Map<String, List<Catelog2Vo>> catalogJsonFromDB;
            try {
                //如果在在死亡时间后业务还没有执行完， 就要增加lock的时间， 这里简单地将时间设为很大的300s
                catalogJsonFromDB = getCatalogJsonFromDB_my();//双重检验+查库+加入缓存
            } finally {
                // lua 脚本解锁,保证原子性: 将lock中的值与uuid比较，然后删除
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                //删除锁
                redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),Arrays.asList("lock"), uuid);
            }
            return catalogJsonFromDB;
        }
        else{
            System.out.println("获取分布式锁失败...等待重试...");
            //休眠100ms
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatalogJsonFromDBResdisLock(); //自旋

        }
    }


    /**
     * 本地加锁
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBLocalLock() {
        synchronized (this){
            //查数据库
            Map<String, List<Catelog2Vo>> catalogJsonFromDB = getCatalogJsonFromDB_my();
            return catalogJsonFromDB;
        }
    }

    /**
     * 需要拿到锁之后再查
     * 读取数据库的操作， 并加入到缓存
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDB_my() {
        //拿到锁后进行双重检验，如果缓存中有数据，直接取
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if(!StringUtils.isEmpty(catalogJson)){
            Map<String, List<Catelog2Vo>> stringListMap = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {});
            return stringListMap;
        }

        System.out.println("查询了数据库");

        // 性能优化：将数据库的多次查询变为一次
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);

        //1、查出所有分类
        //1、1）查出所有一级分类
        List<CategoryEntity> level1Categories = getParentCid(selectList, 0L);

        //封装数据
        Map<String, List<Catelog2Vo>> parentCid = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1、每一个的一级分类,查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());

            //2、封装上面的结果
            List<Catelog2Vo> Catelog2Vos = null;
            if (categoryEntities != null) {
                Catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo Catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName().toString());

                    //1、找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParentCid(selectList, l2.getCatId());

                    if (level3Catelog != null) {
                        List<Catelog2Vo.Category3Vo> category3Vos = level3Catelog.stream().map(l3 -> {
                            //2、封装成指定格式
                            Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                            return category3Vo;
                        }).collect(Collectors.toList());
                        Catelog2Vo.setCatalog3List(category3Vos);
                    }

                    return Catelog2Vo;
                }).collect(Collectors.toList());
            }
            return Catelog2Vos;
        }));
        //将查到的数据转换为json存入缓存
        String jsonString = JSON.toJSONString(parentCid);
        redisTemplate.opsForValue().set("catalogJson", jsonString, 1, TimeUnit.DAYS);
        return parentCid;
    }

    /**
     *
     * 读取数据库的操作
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDB() {

        System.out.println("查询了数据库");

        // 性能优化：将数据库的多次查询变为一次
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);

        //1、查出所有分类
        //1、1）查出所有一级分类
        List<CategoryEntity> level1Categories = getParentCid(selectList, 0L);

        //封装数据
        Map<String, List<Catelog2Vo>> parentCid = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1、每一个的一级分类,查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());

            //2、封装上面的结果
            List<Catelog2Vo> Catelog2Vos = null;
            if (categoryEntities != null) {
                Catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo Catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName().toString());

                    //1、找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParentCid(selectList, l2.getCatId());

                    if (level3Catelog != null) {
                        List<Catelog2Vo.Category3Vo> category3Vos = level3Catelog.stream().map(l3 -> {
                            //2、封装成指定格式
                            Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                            return category3Vo;
                        }).collect(Collectors.toList());
                        Catelog2Vo.setCatalog3List(category3Vos);
                    }

                    return Catelog2Vo;
                }).collect(Collectors.toList());
            }
            return Catelog2Vos;
        }));
        return parentCid;
    }

    private List<CategoryEntity> getParentCid(List<CategoryEntity> selectList, Long parentCid) {
        return selectList.stream().filter(item -> item.getParentCid().equals(parentCid)).collect(Collectors.toList());
    }
}