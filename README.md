#phrackCTF-Platform-Personal

This is the full version of phrackCTF-Platform including backend & frontend. This platform is for personal competition, user register as single person and take part in competition. Team version is here:
虽然我不能保证这是前端最炫的CTF平台，这是管理功能最强大的CTF平台。

Based on Spring and SpringMVC framework.
##Techniques

###Base Framework
spring & spring MVC
###Database Connect Pool:
Alibaba Druid
###SQL mapper framework
Mybatis
###Security framework
Apache Shrio

###Database
postgresql-9.5

###FrontEnd
Bootstrap & jQuery

##Notice
It's highly recommanded to use https when deploy this platform.
Before using :
1. Set mail server info in resources/spring-mail.xml because this platform using mail system to activate user.
2. Set database information in system.properties
3. Mail template in top.phrack.ctf.utils.MailUtil

##License
Apache Public License v2.