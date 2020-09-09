package com.twu;

public class Main {

    public static void main(String[] args) {
        private Logger logger = LoggerFactory.getLogger(this.getClass());

        @Resource(name = "redisKeyDatabase")
        private RedisTemplate redisKeyDatabase;
    
        @Resource(name = "redisKeyTimeDatabase")
        private RedisTemplate redisKeyTimeDatabase;
    
        @Autowired
        private BookInfoService bookInfoService;
    
       
        @RequestMapping(value = "/add", method = RequestMethod.POST)
        public JsonResult redisAdd() {
            List<BookInfo> bookInfos = bookInfoService.getBookInfos();
            for (BookInfo bookInfo : bookInfos) {
                if (StringUtils.isNotBlank(bookInfo.getAuthor())) {
                    String author = bookInfo.getAuthor().replaceAll("\\﹝.*?\\﹞|\\〔.*?\\〕|\\(.*?\\)|\\{.*?}|\\[.*?]|\\［.*?］|\\【.*?】|（.*?）|等|著|编者|主编|[^0-9a-zA-Z\u4e00-\u9fa5.，,•·《 》]", "").trim();
                    author = author.replaceAll("\\s+", " ").trim();
                    try {
                        if (author.endsWith(",") || author.endsWith("，")) {
                            author = author.substring(0, author.length() - 1);
                        }
                    } catch (Exception e) {
                        logger.error("1111111" + bookInfo.toString());
                    }
                    try {
                        String tem = author.substring(author.lastIndexOf(",") + 1, author.length());
                        String tem1 = author.substring(author.lastIndexOf("，") + 1, author.length());
                        if (StringUtils.isBlank(tem)) {
                            author = author.substring(0, author.lastIndexOf(","));
                        }
                        if (StringUtils.isBlank(tem1)) {
                            author = author.substring(0, author.lastIndexOf("，"));
                        }
                    } catch (Exception e) {
                        logger.error("2222222" + bookInfo.toString());
                    }
                    bookInfo.setAuthor(author);
                    bookInfoService.updateByPrimaryKeySelective(bookInfo);
                }
            }
            Long now = System.currentTimeMillis();
            ZSetOperations zSetOperations = redisKeyDatabase.opsForZSet();
            ValueOperations<String, Long> valueOperations = redisKeyTimeDatabase.opsForValue();
            List<String> title = bookInfoService.getBookTitle();
            List<String> author = bookInfoService.getBookAuthor();
            for (int i = 0, lengh = title.size(); i < lengh; i++) {
                String tle = title.get(i);
                try {
                    if (zSetOperations.score("title", tle) <= 0) {
                        zSetOperations.add("title", tle, 0);
                        valueOperations.set(tle, now);
                    }
                } catch (Exception e) {
                    zSetOperations.add("title", tle, 0);
                    valueOperations.set(tle, now);
                }
            }
            for (int i = 0, lengh = author.size(); i < lengh; i++) {
                String aut = author.get(i);
                if (StringUtils.isNotBlank(aut)) {
                    String auth[] = aut.split(",|，");
                    for (String str : auth) {
                        if (StringUtils.isNotBlank(str)) {
                            try {
                                if (zSetOperations.score("title", str.trim()) <= 0) {
                                    zSetOperations.add("title", str.trim(), 0);
                                    valueOperations.set(str.trim(), now);
                                }
                            } catch (Exception e) {
                                zSetOperations.add("title", str.trim(), 0);
                                valueOperations.set(str.trim(), now);
                            }
                        }
                    }
                }
            }
            return new JsonResult(ResultCode.SUCCESS, "成功");
    }
}
