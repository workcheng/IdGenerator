package tid

/**
 * 正数的进制转换工具
 * @author chenghui
 * @date 2024/5/6 9:00
 */
public class BinaryCompressUtil {
    public static void main(String[] args) {
        System.out.println("四位高进制最高表示数字:"+reduction("ZZZZ"));
        for (int i = 0; i < 999; i++) {
            final String x = scaleTransition(i);
            final Integer reductionResult = reduction(x);
            final String complete = complete(6, i);
            System.out.println(i + "," + x + "," + String.valueOf(reductionResult)  + "," + (i == reductionResult) + "," + complete+ "," + reduction(complete));
        }
    }

    /**
     * 将数字表示为字符串的所有可能字符
     */
//    private static final String UNIT_STR = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ@#$%^&*()_+={}[]:;<>";//82进制 四位表示数字最大值:45212175
    private static final String UNIT_STR = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";//62进制 四位表示数字最大值:14776335
    /**
     * 表示使用的是多少位进制
     */
    private static final Integer SCALE;
    /**
     * 根据 UNIT_STR 生成的 char array
     */
    private static final char[] UNIT_ARRAY;
    /**
     * 当前字符在当前进制中表示的大小 占用空间极少,但可以大大减少程序循环次数,有效提升运行效率
     */
    private static final Map<Character,Integer> UNIT_HOW_MANY_MAP;
    static {
        UNIT_ARRAY = UNIT_STR.toCharArray();
        SCALE = UNIT_STR.length();
        UNIT_HOW_MANY_MAP = new HashMap<>();
        for (int index = 0; index < UNIT_ARRAY.length; index ++){
            UNIT_HOW_MANY_MAP.put(UNIT_ARRAY[index],index);
        }
    }

    /**
     * @date 2024-01-05
     * 进制转换 把十进制的number转换为SCALE进制 或者可以理解为把很长的数字使用字母代替
     */
    public static String scaleTransition(Integer number) {
        StringBuilder result = new StringBuilder();
        while (number / SCALE > 0) {
            result.append(UNIT_ARRAY[number % SCALE]);
            number = number / SCALE;
        }
        result.append(UNIT_ARRAY[number]);
        return result.reverse().toString();
    }

    /**
     * @date 2024-01-05
     * 把通过 scaleTransition(int)方法转换的数字重新转换的数字转换为十进制
     */
    public static Integer reduction(String number){
        int result = 0;
        for (int index = 0; index <number.length(); index ++){
            int pow = (int)Math.pow(SCALE,number.length() - index - 1);
            Integer howMany = UNIT_HOW_MANY_MAP.get(number.charAt(index));
            result += howMany * pow;
        }
        return result;
    }

    /**
     * 指定长度的进制数的最大值
     * @param length
     * @return 长度length的字符串最大值
     */
    public static String maxString(Integer length) {
        final String s = UNIT_STR.substring(UNIT_STR.length() - 1);
        StringBuilder sb = new StringBuilder();
        for (int index = 0; index < length; index ++){
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * 指定长度进制数的10进制的最大值
     * @param length
     * @return length长度的10进制的最大值
     */
    public static Integer maxInteger(Integer length) {
        return reduction(maxString(length));
    }

    /**
     * 补全长度
     * @param length 指定长度
     * @param raw 原数
     * @return 补全length长度的进制数
     */
    public static String complete(Integer length, int raw) {
        final String maxString = maxString(length);
        final Integer reduction = reduction(maxString);
        if (raw > reduction){
            throw new RuntimeException("大小超过进制数最大值:" + reduction);
        }
        final String scaled = scaleTransition(raw);
        return complete(length, scaled);
    }

    public static String complete(Integer length, String scaled) {
        if (scaled.length() > length) {
            throw new RuntimeException("原数长度超过了指定长度:" + scaled.length());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(scaled);
        int temp = length - sb.length();
        if (temp > 0) {
            //若长度不足进行补零
            while (sb.length() < length) {
                //每次都在最前面补零
                sb.insert(0, "0");
            }
        }
        return sb.toString();
    }
}
