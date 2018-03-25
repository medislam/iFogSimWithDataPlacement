#include <jni.h>
#include <stdio.h>
#include "org_cloudbus_cloudsim_network_FloydWarshallFloatPrallel.h"
#include <float.h>
#include <omp.h>
#include <stdlib.h>
#include <limits.h>


#define Oriented 1 /* if oriented graph T <-1, else T<- 0 */

jint **flow;
jfloat **dist_f;

void printDist(jint n,jfloat **dis) {
	int i, j;

	printf("\t");
	for (i = 0; i < n; ++i)
		printf("\t%d", i);

	printf("\n");
	for (i = 0; i < n; ++i) {
		printf("\t%d", i);
		for (j = 0; j < n; ++j){
			if (dis[i][j]==FLT_MAX)
				printf("\t%4s", "Inf");
			else
				printf("\t%4.1f", dis[i][j]);
		}
		printf("\n");
	}
	printf("\n");
}

void printFlow(jint n,jint **flw) {
	int i, j;

	printf("\t");
	for (i = 0; i < n; ++i)
		printf("\t%d", i);

	printf("\n");
	for (i = 0; i < n; ++i) {
		printf("\t%d", i);
		for (j = 0; j < n; ++j){
			if (flw[i][j]==INT_MAX)
				printf("\t%4s", "Inf");
			else
				printf("\t%4d", flw[i][j]);
		}
		printf("\n");
	}
	printf("\n");
}

void init_dist_f_flow(jint n, jfloat** dis){
	jint i,j;
	dist_f = (jfloat**) malloc(n*sizeof(jfloat*));
	flow   = (jint**) malloc(n*sizeof(jint*));

	for(i = 0; i < n; ++i){
		dist_f[i] = (jfloat*) malloc(n*sizeof(jfloat));
		flow[i]   = (jint*) malloc(n*sizeof(jint));

		for(j = 0; j < n; ++j){
			//creation d'une sauvegarde de dist
			if(i==j){
				dist_f[i][j] = 0;
				flow[i][j] = 0;

			}else if(dis[i][j]!=FLT_MAX){
				dist_f[i][j]=dis[i][j];
				flow[i][j]=i;

			}else{
				dist_f[i][j]=INT_MAX;
				flow[i][j]=INT_MAX;
			}
		}
	}
}

void floyd_warshall(jint n) {
	int i, j, k;

	for (k = 0; k < n; ++k)
    	#pragma omp parallel for private(i,j)
		for (i = 0; i < n; ++i){
			register float r = dist_f[i][k];
			if ( r == FLT_MAX){
				continue;
			}

			if (i==k){
				continue;
			}
			for (j = 0; j < n; ++j){
				//if (dist_f[i][k] < dist_f[i][j])
				//if(dist_f[k][j] < dist_f[i][j]){
				if(dist_f[i][j] > r +dist_f[k][j] ){
					dist_f[i][j] = r +dist_f[k][j];
					flow[i][j] = flow[k][j];
				}
				//}
			}
		}
}

/*
void floyd_warshall2(jint n) {
int i, j, k,v;

for (k = 0; k < n; ++k)
#pragma omp parallel for private(i,j)
for (i = 0; i < n; ++i){
for (j = 0; j < n; ++j){
if (dist_f[i][k] == FLT_MAX || dist_f[k][j] == FLT_MAX)
continue;
if(dist_f[i][j] > dist_f[i][k]+dist_f[k][j]){
dist_f[i][j] = dist_f[i][k]+dist_f[k][j];
flow[i][j] = flow[k][j];
}
}
}
}
*/


//JNIEXPORT void JNICALL Java_HelloWorld_floyd(JNIEnv *env, jobject obj, jint n,jint m, jobjectArray dist){
JNIEXPORT void JNICALL  Java_org_cloudbus_cloudsim_network_FloydWarshallFloatPrallel_floyd(JNIEnv *env, jobject obj, jint n,jint m, jobjectArray dist, jobject lock){
	double start,stop,start2,stop2;

	int i, asize;
	jfloat** localArray;
	jfloatArray* oneDim; 
	jobjectArray ret,ret2;
	jclass cls;
	jmethodID mid;
	jfieldID fid;
	jint arraySize;
	//n -> nombre de noeuds, m-> nombre d'arcs et dist -> la matrice de distances
	//recupération des données de distances
	printf("n=%d\n",n);
	printf("m=%d\n",m);

	oneDim = malloc(n*sizeof(jfloatArray*));
	localArray = malloc(n*sizeof(jfloat*));

	for (i = 0; i < n; ++i){
		oneDim[i] = (*env)->GetObjectArrayElement(env,dist,i);
		localArray[i] = (*env)->GetFloatArrayElements(env,oneDim[i],NULL);
	}


	//printf("\t%s\n","---------------- dist ------------------------" );
	//printDist(n,localArray);

	//initialiser la matrice des shortest paths + la matrice flow
	init_dist_f_flow(n, localArray);

	//calcul des shortest paths
	start2 = omp_get_wtime();
	floyd_warshall(n);
	stop2 = omp_get_wtime();
	printf("\ntime floyd_warshall parallel = %f\n", stop2 - start2);

	//printf("\t%s\n","--------------  dist_f  ----------------------" );
	//printDist(n,dist_f);


	//printf("\t%s\n","---------------- flow ------------------------" );
	//printFlow(n,flow);

	/*
	* return the shortest paths values 
	*/
	jfloatArray row = (jfloatArray)(*env)->NewFloatArray(env,n);
	ret=(jobjectArray)(*env)->NewObjectArray(env,n,(*env)->GetObjectClass(env,row),0);

	for(i=0;i<n;i++){
		row = (jfloatArray)(*env)->NewFloatArray(env,n);
		(*env)->SetFloatArrayRegion(env,(jfloatArray)row,(jsize)0,n,(jfloat *)dist_f[i]);
		(*env)->SetObjectArrayElement(env,ret,i,row);
	}

	cls=(*env)->GetObjectClass(env,obj);
	mid=(*env)->GetMethodID(env,cls,"sendArrayResults","([[F)V");

	if(mid == 0){
		printf("Can't find method sendArrayResults\n");
		return;
	}

	(*env)->ExceptionClear(env);
	(*env)->MonitorEnter(env,lock);
	(*env)->CallVoidMethod(env,obj, mid, ret);
	(*env)->MonitorExit(env,lock);

	if((*env)->ExceptionOccurred(env)){
		printf("Error occured copying array back\n");
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	fid=(*env)->GetFieldID(env,cls,"arraySize","I");
	if(fid==0){
		printf("Can't find field arraySize\n");
		return;
	}

	asize=(*env)->GetIntField(env,obj,fid);
	if(!(*env)->ExceptionOccurred(env)){
		printf("Java array size=%d\n", asize);
	}else{
		(*env)->ExceptionClear(env);
	}


	/*
	* return the shortest paths values and the flow matrix
	*/
	jintArray row2 = (jintArray)(*env)->NewIntArray(env,n);
	ret2 = (jobjectArray)(*env)->NewObjectArray(env,n,(*env)->GetObjectClass(env,row2),0);

	for(i=0;i<n;i++){
		row2 = (jintArray)(*env)->NewIntArray(env,n);
		(*env)->SetIntArrayRegion(env,(jintArray)row2,(jsize)0,n,(jint *)flow[i]);
		(*env)->SetObjectArrayElement(env,ret2,i,row2);
	}

	cls=(*env)->GetObjectClass(env,obj);
	mid=(*env)->GetMethodID(env,cls,"sendArrayResultsFlow","([[I)V");

	if(mid == 0){
		printf("Can't find method sendArrayResultsFlow\n");
		return;
	}

	(*env)->ExceptionClear(env);
	(*env)->MonitorEnter(env,lock);
	(*env)->CallVoidMethod(env,obj, mid, ret2);
	(*env)->MonitorExit(env,lock);

	if((*env)->ExceptionOccurred(env)){
		printf("Error occured copying array back\n");
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	// fid=(*env)->GetFieldID(env,cls,"arraySize","I");
	// if(fid==0){
	// printf("Can't find field arraySize\n");
	// return;
	// }

	// asize=(*env)->GetIntField(env,obj,fid);
	// if(!(*env)->ExceptionOccurred(env)){
	// printf("Java array size=%d\n", asize);
	// }else{
	// (*env)->ExceptionClear(env);
	// }
	printf("END!\n");
	return;

}