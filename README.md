Paper2Swf
=========

<h2>项目介绍</h2>
convert office to pdf and pdf to swf.
<ul>
<li>支持word、excel、ppt等office文档格式转swf;</li>
<li>支持pdf转swf.</li>
</ul>

<h2>主要实现思路</h2>
若是Office文档转swf，需要先把Office文档转为pdf文档，然后再使用swftools将pdf转为swf。<br/>
Office文档转pdf的实现方式有两种，一是使用com bridge，使用开源Jacob工具，调用Office或WPS的VBA接口，实现Office文档输出为pdf文档。二是使用OpenOffice、JODConvert将Office文档输出为pdf。<br/>
本方案还尝试提供与gearman结合的实现方式，以更好地支持异步转换。

<h2>参考资料</h2>
<ul>
<li>Jacob office site: http://danadler.com/jacob/</li>
<li>OpenOffice office site: https://www.openoffice.org/</li>
<li>WPS office site: http://www.wps.cn/ </li>
<li>swftools: http://www.swftools.org/download.html </li>
<li>xpdf: ftp://ftp.foolabs.com/pub/xpdf/xpdf-3.03.tar.gz xpdf的作用是增加对语言的支持，比如中文，可从网上搜索资料配置xpdf。</li>
</ul>

<h2>运行环境</h2>
<ul>
<li>swftools、xpdf语言包</li>
<li>若选用com bridge（即jacob）方式，需要安装MS Office 2007或WPS</li>
<li>若使用WPS，默认安装的程序是不带VBA的，需要自行安装Microsoft Visual Basic for Application，然后才能使用。</li>
<li>若选用OpenOffice方式，需要安装OpenOffice(推荐)</li>
<li>若需要更好地支持异步转换，需要部署gearman</li>
</ul>

<h2>存在问题</h2>
<ul>
<li>使用OO转换大文件性能不理想，100MB doc文档大概需要5分钟时间（x86，i5-2.6GHz，8G，win7），使用com bridge快很多</li>
</ul>

<h2>Start OpenOffice as service</h2>
soffice -headless -accept="socket,host=127.0.0.1,port=8100;urp;" -nofirststartwizard
<p>see also: http://www.artofsolving.com/node/10</p>
