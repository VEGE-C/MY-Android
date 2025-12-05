package com.example.vege

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.vege.ui.theme.VEGETheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VEGETheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GradeQueryScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun GradeQueryScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val studentId = remember { mutableStateOf("") }
    val loading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf("") }
    val grades = remember { mutableStateOf(listOf<Map<String, Any?>>()) }
    Column(modifier = modifier.padding(16.dp)) {
        OutlinedTextField(
            value = studentId.value,
            onValueChange = { studentId.value = it },
            label = { Text("学号") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            if (studentId.value.isBlank()) {
                error.value = "请输入学号"
            } else {
                error.value = ""
                loading.value = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { queryGrades(studentId.value) }
                    when {
                        result.isFailure -> {
                            error.value = result.exceptionOrNull()?.message ?: "查询失败"
                            grades.value = emptyList()
                        }
                        else -> {
                            grades.value = result.getOrNull() ?: emptyList()
                        }
                    }
                    loading.value = false
                }
            }
        }) {
            Text(if (loading.value) "查询中..." else "查询成绩")
        }
        if (error.value.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error.value)
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn {
            items(grades.value) { item ->
                val kcmc = item["kcmc"]?.toString() ?: ""
                val cj = item["cj"]?.toString() ?: item["kscj"]?.toString() ?: ""
                val xfjd = item["xfjd"]?.toString() ?: ""
                Text("课程: $kcmc  成绩: $cj  绩点: $xfjd")
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GradeQueryPreview() {
    VEGETheme {
        GradeQueryScreen()
    }
}

private fun postForm(urlStr: String, headers: Map<String, String>, form: Map<String, String>): Result<String> {
    return try {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            for ((k, v) in headers) setRequestProperty(k, v)
        }
        val body = form.entries.joinToString("&") { "${it.key}=${it.value}" }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = BufferedReader(stream.reader(Charsets.UTF_8)).use { it.readText() }
        if (code in 200..299) Result.success(text) else Result.failure(Exception(text))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun queryGrades(gh: String): Result<List<Map<String, Any?>>> {
    val ua = "Mozilla/5.0 (Linux; Android 15; V2232A Build/AP3A.240905.015.A2; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/138.0.7204.180 Mobile Safari/537.36 XWEB/1380215 MMWEBSDK/20250202 MMWEBID/140 wxwork/5.0.0 MicroMessenger/7.0.1 NetType/4G Language/zh Lang/zh ColorScheme/Light wwmver/3.26.500.650"
    val tokenHeaders = mapOf("User-Agent" to ua)
    val tokenRes = postForm("http://jwqywx.cczu.edu.cn:8180/api/gh_get_yhdm", tokenHeaders, mapOf("gh" to gh))
    if (tokenRes.isFailure) return Result.failure(tokenRes.exceptionOrNull() ?: Exception("token error"))
    val tokenJson = JSONObject(tokenRes.getOrNull() ?: return Result.failure(Exception("token empty")))
    val token = tokenJson.optString("token")
    val msgArr = tokenJson.optJSONArray("message") ?: JSONArray()
    if (msgArr.length() == 0) return Result.failure(Exception("未找到账号"))
    val xh = msgArr.getJSONObject(0).optString("yhid")
    val gradeHeaders = mapOf(
        "Authorization" to "Bearer $token",
        "User-Agent" to ua
    )
    val gradeRes = postForm("http://jwqywx.cczu.edu.cn:8180/api/cj_xh", gradeHeaders, mapOf("xh" to xh))
    if (gradeRes.isFailure) return Result.failure(gradeRes.exceptionOrNull() ?: Exception("grade error"))
    val gradeJson = JSONObject(gradeRes.getOrNull() ?: return Result.failure(Exception("grade empty")))
    val gradeMsg = gradeJson.optJSONArray("message") ?: JSONArray()
    val items = mutableListOf<Map<String, Any?>>()
    for (i in 0 until gradeMsg.length()) {
        val obj = gradeMsg.getJSONObject(i)
        val map = mutableMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = obj.opt(k)
        }
        items.add(map)
    }
    return Result.success(items)
}
