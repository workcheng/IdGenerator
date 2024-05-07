package tid

import cn.hutool.extra.spring.SpringUtil
import com.google.common.collect.Lists
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.serializer.RedisSerializer

/**
 * @author chenghui
 * @date 2024/5/6 10:14
 */
class IdGenerator {
    private static final Logger logger = LoggerFactory.getLogger("IdGenerator.groovy")

    public static final String COLON = ":";

    private static InitParams initParams;

    static {
        logger.info("static initParams")
        init();
    }

    // 构造函数
    public static void initParams(String key, String initFields) {
        initParams = parse(key, initFields);
    }

    private static void init() {
        def key = "IdGenerator"
        def initValue = 1;
//        initParams(key, "XM:1:14776335:30:50:100:1")
        initParams(key, "XM:" + initValue + ":9:30:50:100:1")
    }

    /**
     *  解析表达式   字段名:初始值:最大值:最小数量:扩容数量:初始数量:增长步长
     *
     * @param initField
     */
    private static InitParams parse(String key, String initField) {
        InitParams initParams = new InitParams();
        if (StringUtils.contains(initField, COLON)) {
            String[] params = StringUtils.splitByWholeSeparatorPreserveAllTokens(initField, COLON);
            initParams.setFieldName(key + COLON + params[0]);
            def fieldName = initParams.getFieldName()
            if (!exist(fieldName)) {
                logger.info("key not exists:{}", fieldName)
                // 不存在，取数据库中的最大值
                def code = getLastCode()
                if (StringUtils.isNotEmpty(code)) {
                    def reduction = BinaryCompressUtil.reduction(code)
                    params[1] = reduction + 1
                    logger.info("initValue:{},code:{},reduction:{}", params[1], code, reduction)
                }
            }
            logger.info("initValue:{}", params[1])
            initParams.setField(params);
        } else {
            initParams.setFieldName(key + COLON + initField);
            initParams.updateFields();
        }
        return initParams;
    }

    // 执行lua脚本，生成对应的自增id
    public String generate() {
        if (initParams == null) {
            logger.info("initParams")
            //initParams("IdGenerator", "XM:1:14776335:30:50:100:1")
            init()
        }
        final InitParams initParams = initParams;
        String fieldName = initParams.getFieldName();
        return executeLua(
                fieldName,
                initParams.getInitValue(),
                initParams.getMaxValue(),
                initParams.getMinCount(),
                initParams.getInitCount(),
                initParams.getExpansionStep(),
                initParams.getIncrStep());
    }

    // 执行lua脚本，生成对应的自增id
    public cn.hutool.json.JSONObject generate(cn.hutool.json.JSONObject jsonObject) {
        def result = this.generate()
        jsonObject.put("result", result)

        def codes = BinaryCompressUtil.complete(6, Integer.valueOf(result))
        jsonObject.put("result code", codes)
        //DataFunc.insert("insert into D_JH_BHXM(FGDH,FBH,FXM,SYS_FWRITETIME) values ('"+ codes +"','"+ codes +"','"+ codes +"',current_date)", new HashMap<String, Object>())
        return jsonObject;
    }

    // 执行lua脚本
    private String executeLua(String key, int initValue, int maxValue, int minCount, int initCount, int expansionStep, int incrStep) {
        if (!exist(key)) {
            logger.info("key not exists:{}", key)
            // 不存在，取数据库中的最大值
            def code = getLastCode()
            if (StringUtils.isNotEmpty(code)) {
                def reduction = BinaryCompressUtil.reduction(code)
                initValue = reduction + 1
                logger.info("initValue:{},code:{},reduction:{}", initValue, code, reduction)
            }
        }
        logger.info("initValue:{}", initValue)
        // 执行lua脚本
        def list = new ArrayList<String>()
        list.add(CastUtil.castString(initValue))
        list.add(CastUtil.castString(maxValue))
        list.add(CastUtil.castString(minCount))
        list.add(CastUtil.castString(initCount))
        list.add(CastUtil.castString(expansionStep))
        list.add(CastUtil.castString(incrStep));
        def instance = SpringUtil.getBean(StringRedisTemplate.class)
        def keys = new ArrayList<String>()
        keys.add(key)
        //String result = CastUtil.castString(instance.getResource().eval(LUA_SCRIPT, keys, list))
        // 执行lua脚本
        DefaultRedisScript<String> defaultRedisScript = new DefaultRedisScript<>();
        defaultRedisScript.setResultType(String.class);
        defaultRedisScript.setScriptText(LUA_SCRIPT);
        RedisSerializer<String> serializer = instance.getStringSerializer();
        String result =
                com.example.spring.id.CastUtil.castString(
                        instance.execute(
                                defaultRedisScript,
                                serializer,
                                serializer,
                                Lists.newArrayList(key),
                                com.example.spring.id.CastUtil.castString(initValue),
                                com.example.spring.id.CastUtil.castString(maxValue),
                                com.example.spring.id.CastUtil.castString(minCount),
                                com.example.spring.id.CastUtil.castString(initCount),
                                com.example.spring.id.CastUtil.castString(expansionStep),
                                com.example.spring.id.CastUtil.castString(incrStep)));
        return result;
    }

    private static boolean exist(String key) {
        return SpringUtil.getBean(StringRedisTemplate.class).hasKey(key)
    }

    private static class InitParams {
        /** 默认初始值 */
        private static final int DEFAULT_INIT_VALUE = 1;
        /** 默认最大值 */
        private static final int DEFAULT_MAX_VALUE = 9999;
        /** 默认最小数量 */
        private static final int DEFAULT_MIN_COUNT = 30;
        /** 默认扩容数量 */
        private static final int DEFAULT_EXPANSION_STEP = 50;
        /** 默认初始数量 */
        private static final int DEFAULT_INIT_COUNT = 100;
        /** 默认自增步长 */
        private static final int DEFAULT_INCR_STEP = 1;

        private final int[] params = [
                0,
                DEFAULT_INIT_VALUE,
                DEFAULT_MAX_VALUE,
                DEFAULT_MIN_COUNT,
                DEFAULT_EXPANSION_STEP,
                DEFAULT_INIT_COUNT,
                DEFAULT_INCR_STEP
        ];
        /** 字段名称，其实就是key */
        private String fieldName;
        /** 初始值 */
        private int initValue;
        /** 最大值 */
        private int maxValue;
        /** 最小数量 */
        private int minCount;
        /** 扩容步长 */
        private int expansionStep;
        /** 初始数量 */
        private int initCount;
        /** 自增步长 */
        private int incrStep;

        int[] getParams() {
            return params
        }

        String getFieldName() {
            return this.fieldName
        }

        void setFieldName(String fieldName) {
            this.fieldName = fieldName
        }

        int getInitValue() {
            return initValue
        }

        void setInitValue(int initValue) {
            this.initValue = initValue
        }

        int getMaxValue() {
            return maxValue
        }

        void setMaxValue(int maxValue) {
            this.maxValue = maxValue
        }

        int getMinCount() {
            return minCount
        }

        void setMinCount(int minCount) {
            this.minCount = minCount
        }

        int getExpansionStep() {
            return expansionStep
        }

        void setExpansionStep(int expansionStep) {
            this.expansionStep = expansionStep
        }

        int getInitCount() {
            return initCount
        }

        void setInitCount(int initCount) {
            this.initCount = initCount
        }

        int getIncrStep() {
            return incrStep
        }

        void setIncrStep(int incrStep) {
            this.incrStep = incrStep
        }

        public void setField(Object[] objects) {
            if (ArrayUtils.isEmpty(objects) || ArrayUtils.getLength(objects) < 2) {
                return;
            }
            for (int i = 1; i < objects.length; i++) {
                Object obj = objects[i];
                params[i] = CastUtil.castInt(obj);
            }
            updateFields();
        }

        public void updateFields() {
            this.initValue = params[1];
            this.maxValue = params[2];
            this.minCount = params[3];
            this.expansionStep = params[4];
            this.initCount = params[5];
            this.incrStep = params[6];
        }

        @Override
        public String toString() {
            return "InitParams{" +
                    "params=" + Arrays.toString(params) +
                    ", fieldName='" + this.fieldName + '\'' +
                    ", initValue=" + initValue +
                    ", maxValue=" + maxValue +
                    ", minCount=" + minCount +
                    ", expansionStep=" + expansionStep +
                    ", initCount=" + initCount +
                    ", incrStep=" + incrStep +
                    '}';
        }
    }

    // 该脚本的执行逻辑
    // 在redis中生成一个队列，指定初始化长度，第一个初始值，最大值，队列最小数量，每次扩容的数量，自增的步长
    // 1.如果队列不存在，就初始化队列，按照给定的初始化长度，初始值，自增步长，最大值等参数创建一个队列
    // 2.如果队列中值的数量超过队列最小数量，那么直接pop出一个值
    // 3.如果小于最小数量，那么直接循环生成指定步长的自增ID
    // 4.最终会pop出第一个数值
    //private static final String LUA_SCRIPT = "local key=KEYS[1]\nlocal initValue=tonumber(ARGV[1])\nlocal maxValue=tonumber(ARGV[2])\nlocal minCount=tonumber(ARGV[3])\nlocal initCount=tonumber(ARGV[4])\nlocal expansionStep=tonumber(ARGV[5])\nlocal incrStep=tonumber(ARGV[6])\nlocal len=redis.call('llen',key)\nlocal isInit=true\nlocal loop=initCount\nlocal nextValue=initValue\nif len>minCount\nthen\nreturn redis.call('lpop',key)\nend\nif len>0\nthen\nisInit=false\nloop=len+expansionStep\nnextValue=tonumber(redis.call('rpop',key))\nend\nwhile(len<loop)\ndo\nif nextValue>maxValue\nthen\nnextValue=initValue\nend\nredis.call('rpush',key,nextValue)\nnextValue=nextValue+incrStep\nlen=len+1\nend\nif isInit\nthen\nreturn 'success'\nend\nreturn redis.call('lpop',key)";

    private static final String LUA_SCRIPT =
            "local key=KEYS[1]\nlocal initValue=tonumber(ARGV[1])\nlocal maxValue=tonumber(ARGV[2])\nlocal minCount=tonumber(ARGV[3])\nlocal initCount=tonumber(ARGV[4])\nlocal expansionStep=tonumber(ARGV[5])\nlocal incrStep=tonumber(ARGV[6])\nlocal len=redis.call('llen',key)\nlocal isInit=true\nlocal loop=initCount\nlocal nextValue=initValue\nif len>minCount\nthen\nreturn redis.call('lpop',key)\nend\nif len>0\nthen\nisInit=false\nloop=len+expansionStep\nnextValue=tonumber(redis.call('rpop',key))\nend\nwhile(len<loop)\ndo\nif nextValue>maxValue\nthen\nnextValue=initValue\nend\nredis.call('rpush',key,nextValue)\nnextValue=nextValue+incrStep\nlen=len+1\nend\nreturn redis.call('lpop',key)";

    /**
     * 从数据库中查询XM码生成的最大值
     * @return
     */
    private static String getLastCode() {
        /*String sql = "SELECT t.* from (SELECT FXM FROM D_JH_BHXM ORDER BY NLSSORT(FXM,'NLS_SORT = SCHINESE_RADICAL_M') DESC) t WHERE rownum = 1";
        Map<String, Object> param = new HashMap<String, Object>();
        List<Map<String, Object>> xmList = DataFunc.query(sql.toString(), param);
        if (CollectionUtil.isNotEmpty(xmList)) {
            return String.valueOf(xmList.get(0).get("FXM"))
        } else {
            logger.warn("D_JH_BHXM FXM is empty")
            return null
        }*/
        return null;
    }
}
